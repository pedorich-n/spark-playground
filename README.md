# Spark-playground

Some Spark Scala code to process text files for **Active Wizards** test task
What it does:
- Loads files from __/opt/activeWizards/input/*.txt__
- Removes punctuation, numbers, linebreaks, whitespace sequences
- Tokenizes formatted text into words
- Removes stopwords for *English*, *German* and *French* texts
- Lemmatizes __English__ words, leaves words of other languages as they are
- Saves formatted and lemmatized text into __/opt/activeWizards/output/\*filename*.txt__
- Counts word occurences with **CountVectorizer**
- Picks top 30 keywords for file
- And saves into __/opt/activeWizards/output/keywords/\*filename*.txt__

### Used libraries
- Spark Core
- Spark MlLib
- Spark SQL
- Stanford CoreNLP with default model
- Apache Kryo for Serialization in Spark

### Possible improvements
- Move to https://github.com/clulab/processors or other Scala wrapper for CoreNLP
- Lemmatizer for non English languages. Possible tools:
  - https://github.com/languagetool-org/languagetool
  - ElasticSearch
- Get rid of NotSerializableException from Pipeline somehow