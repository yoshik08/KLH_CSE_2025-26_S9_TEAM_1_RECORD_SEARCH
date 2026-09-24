/**
 * Contract for any string matching algorithm used by the field-based search
 * (search by name / class / extracurricular over structured records).
 *
 * Two implementations exist: SimpleStringMatcher (exact substring) and
 * FuzzyStringMatcher (typo-tolerant, via edit distance). Swapping between
 * them only requires changing the one line in Main.java that constructs
 * the matcher — StudentRepository and its search methods never change.
 */
public interface StringMatcher {
    boolean matches(String query, String target);
}
