# Intelligent Student Record Search System

Java console app for DSA-3 Project.

## Structure
```
src/
  Student.java              - data model + toProfileText() (builds corpus sentences)
  StringMatcher.java         - interface for field-search matching algorithm
  SimpleStringMatcher.java   - basic case-insensitive substring match
  StudentRepository.java     - ArrayList + HashMap-indexed storage, field search
  CsvLoader.java              - loads corpus CSV into the repository
  CorpusBuilder.java          - generates text-document corpus (one file per Class)
  PatternSearch.java           - KMP algorithm + runs it across every document in
                                  corpus/, kept in one file since it's one idea
                                  (see comment block at the top of the file for
                                  how to read/explain it during the review)
  Main.java                  - console menu entry point
data/
  student_records.csv        - source corpus (10,000 records, from Kaggle)
corpus/
  class_5.txt ... class_12.txt  - generated text documents (one per Class),
                                    auto-built on every run from the CSV
```

## How to run
```
javac -d bin src/*.java
java -cp bin Main data/student_records.csv
```
The corpus/ text documents are (re)generated automatically each run, so
they always match the current CSV data.

## Menu options
1-4: field search over structured records (name / ID / class / extracurricular)
5:   **KMP pattern search** over the generated text corpus (Review-2 focus)

## Why documents are grouped by Class, not one file per student
10,000 individual files would be unmanageable to demo or search across.
Raw CSV rows aren't real "text documents." Grouping by Class (8 files)
keeps the corpus small enough to inspect directly, while each document
is still a genuine block of prose covering ~1,200-1,300 students, so a
single pattern search still reaches the whole dataset.

## About the KMP algorithm (PatternSearch.java)
Knuth-Morris-Pratt finds every occurrence of a pattern in a text in
O(n + m) time (n = text length, m = pattern length), instead of the
naive algorithm's worst-case O(n*m). It precomputes an LPS ("longest
proper prefix that is also a suffix") array for the pattern. On a
mismatch, instead of restarting the text pointer from scratch like the
naive approach does, KMP uses the LPS array to know how far it can
safely resume the pattern pointer — so it never re-examines a text
character it has already matched.

## Upgrade path (structured field search)
Only SimpleStringMatcher.java needs to change (or add a new class
implementing StringMatcher, e.g. FuzzyStringMatcher using Levenshtein
distance) and update the one line in Main.java that constructs it.
No other file needs to change.
