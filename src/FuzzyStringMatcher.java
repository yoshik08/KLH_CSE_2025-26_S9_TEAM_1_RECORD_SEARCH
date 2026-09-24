/**
 * Fuzzy string matching algorithm using Edit Distance (Levenshtein Distance),
 * computed with dynamic programming.
 *
 * Implements the same StringMatcher interface as SimpleStringMatcher, so it's
 * a drop-in upgrade: only the one line in Main.java that constructs the
 * matcher needs to change. StudentRepository and every search method stay
 * exactly the same.
 *
 * ------------------------------------------------------------------
 * WHAT EDIT DISTANCE MEANS
 * The edit distance between two strings is the minimum number of single
 * character edits (insertions, deletions, substitutions) needed to turn
 * one string into the other. e.g. "kitten" -> "sitting" has edit distance 3.
 *
 * Here it's used for typo-tolerant search: instead of requiring an exact
 * substring match, we check whether the query is "close enough" to some
 * word in the target text, allowing a small number of character differences.
 * ------------------------------------------------------------------
 */
public class FuzzyStringMatcher implements StringMatcher {

    /** Maximum edit distance allowed for a word to still count as a match. */
    private final int threshold;

    public FuzzyStringMatcher() {
        this(2); // default: tolerate up to 2 character differences
    }

    public FuzzyStringMatcher(int threshold) {
        this.threshold = threshold;
    }

    @Override
    public boolean matches(String query, String target) {
        if (query == null || target == null) {
            return false;
        }
        String q = query.trim().toLowerCase();
        String t = target.trim().toLowerCase();
        if (q.isEmpty()) {
            return false;
        }

        // Exact substring match always counts (fast path, no DP needed).
        if (t.contains(q)) {
            return true;
        }

        // Otherwise, check each word in the target individually — comparing
        // the whole query against a long multi-word field would give a huge,
        // meaningless edit distance. Word-by-word comparison is how a real
        // typo-tolerant search (e.g. "Jhon" matching "John") actually works.
        for (String word : t.split("\\s+")) {
            if (editDistance(q, word) <= threshold) {
                return true;
            }
        }
        return false;
    }

    /**
     * Computes the Levenshtein edit distance between two strings using
     * dynamic programming (bottom-up, tabulation).
     *
     * dp[i][j] = edit distance between the first i characters of a and
     * the first j characters of b.
     *
     * Base cases:
     *   dp[0][j] = j  (turn empty string into b's first j chars: j insertions)
     *   dp[i][0] = i  (turn a's first i chars into empty string: i deletions)
     *
     * Transition, for each pair (i, j):
     *   - if a.charAt(i-1) == b.charAt(j-1): no edit needed here,
     *     dp[i][j] = dp[i-1][j-1]
     *   - otherwise, take the cheapest of the three possible edits:
     *     dp[i][j] = 1 + min(
     *         dp[i-1][j],     delete a character from a
     *         dp[i][j-1],     insert a character into a
     *         dp[i-1][j-1]    substitute a character in a
     *     )
     */
    public static int editDistance(String a, String b) {
        int n = a.length();
        int m = b.length();
        int[][] dp = new int[n + 1][m + 1];

        for (int i = 0; i <= n; i++) dp[i][0] = i;
        for (int j = 0; j <= m; j++) dp[0][j] = j;

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    int deleteCost = dp[i - 1][j];
                    int insertCost = dp[i][j - 1];
                    int substituteCost = dp[i - 1][j - 1];
                    dp[i][j] = 1 + Math.min(deleteCost, Math.min(insertCost, substituteCost));
                }
            }
        }
        return dp[n][m];
    }
}
