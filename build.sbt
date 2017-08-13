name := "activeWizards"

version := "0.1"

scalaVersion := "2.11.7"

val sparkVersion = "2.2.0"
val nlpVersion = "3.8.0"

libraryDependencies ++= Seq(
  "com.esotericsoftware.kryo" % "kryo" % "2.24.0",
  "org.apache.hadoop" % "hadoop-client" % "2.7.2", //Forced to use this because of Spark-Guava-Language-detection conflict
  "com.optimaize.languagedetector" % "language-detector" % "0.6",
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-mllib" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "edu.stanford.nlp" % "stanford-corenlp" % nlpVersion,
  "edu.stanford.nlp" % "stanford-corenlp" % nlpVersion classifier "models"
//  "edu.stanford.nlp" % "stanford-corenlp" % nlpVersion classifier "models-german",
//  "edu.stanford.nlp" % "stanford-corenlp" % nlpVersion classifier "models-french"
)

