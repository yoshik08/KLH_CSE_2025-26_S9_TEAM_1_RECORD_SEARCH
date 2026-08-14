# KLH_CSE_2025-26_S9_TEAM_1_RECORD_SEARCH

# Intelligent Student Record Search System

Java console app for DSA-3 Project

## Structure
```
src/
  Student.java             - data model for one student record
  StringMatcher.java        - interface for the matching algorithm (upgrade point)
  SimpleStringMatcher.java  - basic case-insensitive substring match (current implementation)
  StudentRepository.java    - ArrayList + HashMap-indexed storage, search methods
  CsvLoader.java             - loads corpus CSV into the repository
  Main.java                 - console menu entry point
data/
  student_records.csv       - corpus (10,000 records, from Kaggle)
```

## How to run
```
javac -d bin src/*.java
java -cp bin Main data/student_records.csv
```

## Upgrading the string matching algorithm later
Only SimpleStringMatcher.java needs to change (or add a new class implementing
StringMatcher, e.g. FuzzyStringMatcher) and update
the one line in Main.java that constructs the matcher. No other file needs
to change.
