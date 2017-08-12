name := "activeWizards"

version := "0.1"

scalaVersion := "2.11.7"

val sparkVersion = "2.2.0"
val nlpVersion = "3.8.0"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-mllib" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "com.esotericsoftware.kryo" % "kryo" % "2.24.0",
  "edu.stanford.nlp" % "stanford-corenlp" % nlpVersion,
  "edu.stanford.nlp" % "stanford-corenlp" % nlpVersion classifier "models"
//  "edu.stanford.nlp" % "stanford-corenlp" % nlpVersion classifier "models-german",
//  "edu.stanford.nlp" % "stanford-corenlp" % nlpVersion classifier "models-french"
)

