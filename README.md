# Lucene Search Engine

To run the project, enter the following:
```
mvn package
java -jar target/CS7IS3-1.0-SNAPSHOT.jar ANALYZER SIMILARITY
trec_eval/trec_eval -m map -m recall -m P.5 ./cran/QRelsCorrectedforTRECeval ./Results.txt
```

Where:
- `ANALYZER` is a choice of `STANDARD`, `ENGLISH` or `CUSTOM`
- `SIMILARITY` is a choice of `VSM`, `BM25`
