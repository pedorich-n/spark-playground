import java.io.{File, StringReader}
import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets
import java.util.Properties

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.ml.feature._
import edu.stanford.nlp.pipeline._
import edu.stanford.nlp.ling.CoreAnnotations._
import edu.stanford.nlp.process.DocumentPreprocessor
import org.apache.spark.ml.linalg.SparseVector

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import edu.stanford.nlp.pipeline.StanfordCoreNLP


object main {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().
      setAppName("activeWizards").
      setMaster("local")
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .set("spark.kryoserializer.buffer.mb", "24")
    val sc = new SparkContext(conf)
    val spark = SparkSession.builder.appName("activeWizards").getOrCreate()

    //I could use \p{Punct}, but I also need to remove dashes, and I need to leave apostrophes, dots, question and exclamation marks
    val punctuationRegex = """["#$%&()*+,-\/:;<=>@\[\]^_{|}~–‒–—―]"""

        val rootFolder = "/opt/activeWizards"
    val inputFilesPath = s"$rootFolder/input/*.txt"
    val files = sc.wholeTextFiles(inputFilesPath)

    import spark.implicits._
    val sentenceDF = files.map { tup =>
      val (key, value) = tup

      //Need to clear text for a bit before tokenizing to sentences
      val cleared = value.
        replaceAll(punctuationRegex, " "). //Replace all punctuations with whitespaces
        replaceAll("""\d{1,}""", "").      //Remove digits
        replaceAll("""\s{2,}""", " ").     //Replace all whitespace sequences with one whitespace
        replaceAll("""\R""", "").          //Remove line breaks
        replaceAll("""^\s{1,}""", "")      //Remove whitespaces at the beginning of the line

      //This will return Iterator of  sentences which is array of words
      val dp = new DocumentPreprocessor(new StringReader(cleared)).iterator()

      //By this I'm merging all of the words in sentence in one string
      val clearedSentence = dp.map(_.mkString(" "))
      (key, clearedSentence.mkString(" ")) //Now we have map like (fileName -> "words from all sentences from file separated by space")
    }.toDF("file", "sentence")


    /*
      According to the answer https://stackoverflow.com/a/44092036/5408933 by Stanford Professor
      all European languages are tokenized by English rules, so no need in language detection as long
      as it's European languages
    */
    val tokenizer = new Tokenizer().setInputCol("sentence").setOutputCol("words")

    val tokenized = tokenizer.transform(sentenceDF)

    //Stop words arrays small enough to be merged in one array
    val stopWords = StopWordsRemover.loadDefaultStopWords("english") ++
      StopWordsRemover.loadDefaultStopWords("german") ++
      StopWordsRemover.loadDefaultStopWords("french")

    val remover = new StopWordsRemover().setInputCol("words").setOutputCol("filtered")
    remover.setStopWords(stopWords)

    val cleanedWords = remover.transform(tokenized)

    /*
       According to this https://stanfordnlp.github.io/CoreNLP/human-languages.html CoreNLP
       doesn't support lemmatizing non english words. But in future, when it'll be supported, it can be used like this:
       val germanProperties = StringUtils.argsToProperties("-props", "StanfordCoreNLP-german.properties")
       val pipeline = new StanfordCoreNLP(germanProperties)
    */
    val props = new Properties()
    props.put("annotators", "tokenize, ssplit, pos, lemma")
    val lemmatizedWords = cleanedWords.select("file", "filtered").map {
      case Row(fileName: String, words: mutable.WrappedArray[_]) =>

        /*
           Pipeline doesn't support serialization, and recommendations says that creating many pipeliens could take 10-40 sec
           https://stanfordnlp.github.io/CoreNLP/memory-time.html#avoid-creating-lots-of-pipelines
           TODO: avoid somehow
        */
        val pipeline = new StanfordCoreNLP(props)

        val doc = new Annotation(words.mkString(" "))
        pipeline.annotate(doc)
        val lemmas = new ArrayBuffer[String]()
        val sentences = doc.get(classOf[SentencesAnnotation])
        for (sentence <- sentences; token <- sentence.get(classOf[TokensAnnotation])) {
          val lemma = token.get(classOf[LemmaAnnotation])
          if (lemma.length > 2) lemmas += lemma
        }
        (fileName, lemmas)
    }.toDF("file", "lemmas")

    //Plain java.nio.Files because this is easier than spark's saveAsTextFile
    lemmatizedWords.rdd.foreach {
      case Row(fileName: String, lemmas: Seq[_]) =>
        val newFileName = fileName.replace("file:", "").replace("input", "output")
        new File(s"$rootFolder/output").mkdirs()
        Files.write(Paths.get(newFileName), lemmas.mkString(" ").getBytes(StandardCharsets.UTF_8))
    }

    //Let's count occurrences
    val model = new CountVectorizer()
      .setInputCol("lemmas")
      .setOutputCol("features")
      .setMinTF(2)
      .fit(lemmatizedWords)

    val transformed = model.transform(lemmatizedWords)

    //And save result
    transformed.
      select("file", "features").
      rdd.
      foreach {
        case Row(file: String, features: SparseVector) =>
          /*
            SparceVector is an class with array of words indices and their values
            So I'll zip indices and values into array of tuples, sort by values desc, and take top 30 keywords.
            And then I'll use that indices from array of tuple to find according word from vocabulary of Vectorizer model
          */
          val indicesWithValues = features.indices.zip(features.values).sortBy(-_._2).take(30)

          val keywords = indicesWithValues.map { index =>
            s"${index._2.toInt} = ${model.vocabulary(index._1)}"
          }

          val newFileName = file.replace("file:", "").replace("input", "output/keywords")
          new File(s"$rootFolder/output/keywords").mkdirs()
          val header = "Count   Word"
          Files.write(Paths.get(newFileName), s"$header \n${keywords.mkString("\n")}".getBytes(StandardCharsets.UTF_8))
      }

  }
}
