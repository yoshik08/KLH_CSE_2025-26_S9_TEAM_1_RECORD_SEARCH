
import java.io.*;
import java.util.*;

/**
 * Pattern search over the text corpus, using the KMP string matching algorithm.
 *
 * Everything for this feature lives in one file on purpose: the KMP algorithm
 * itself (bottom of the file) and the code that runs it across every document
 * in the corpus/ folder (top of the file) are really one idea — "find this
 * pattern everywhere in our text data" — so there's no need to jump between
 * files to explain it.
 *
 * ------------------------------------------------------------------ HOW TO
 * READ / EXPLAIN THIS FILE 1. search(pattern) - loops over every .txt file in
 * corpus/, reads it line by line, calls kmpSearch() on each line, and collects
 * the matches. 2. kmpSearch(text, pattern) - the actual KMP algorithm. Finds
 * every occurrence of pattern inside text in O(n + m) time instead of the naive
 * algorithm's worst-case O(n*m). 3. buildLpsArray(pattern) - precomputes the
 * "longest proper prefix that is also a suffix" table for the pattern. This is
 * what lets kmpSearch() skip ahead on a mismatch instead of restarting from
 * scratch. ------------------------------------------------------------------
 */
public class PatternSearch {

    /**
     * One match: which file, which line, position within the line, and the line
     * text.
     */
    public static class Match {

        public final String fileName;
        public final int lineNumber;
        public final int position;
        public final String lineText;

        public Match(String fileName, int lineNumber, int position, String lineText) {
            this.fileName = fileName;
            this.lineNumber = lineNumber;
            this.position = position;
            this.lineText = lineText;
        }

        @Override
        public String toString() {
            String snippet = lineText.length() > 110 ? lineText.substring(0, 110) + "..." : lineText;
            return String.format("[%s : line %d, pos %d] %s", fileName, lineNumber, position, snippet);
        }
    }

    private final String corpusDir;

    public PatternSearch(String corpusDir) {
        this.corpusDir = corpusDir;
    }

    /**
     * Searches every text document in the corpus for the given pattern.
     */
    public List<Match> search(String pattern) throws IOException {
        List<Match> results = new ArrayList<>();
        File dir = new File(corpusDir);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (files == null) {
            System.out.println("Warning: corpus folder \"" + corpusDir + "\" not found or empty.");
            return results;
        }

        java.util.Arrays.sort(files); // consistent, predictable order for the demo

        for (File file : files) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                int lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    List<Integer> positions = kmpSearch(line, pattern);
                    for (int pos : positions) {
                        results.add(new Match(file.getName(), lineNumber, pos, line));
                    }
                }
            }
        }
        return results;
    }

    // ------------------------------------------------------------------
    // KMP algorithm
    // ------------------------------------------------------------------
    /**
     */
    private static List<Integer> kmpSearch(String text, String pattern) {
        List<Integer> matches = new ArrayList<>();
        if (text == null || pattern == null || pattern.isEmpty() || text.isEmpty()) {
            return matches;
        }

        int[] lps = buildLpsArray(pattern);
        int i = 0; // index for text
        int j = 0; // index for pattern

        while (i < text.length()) {
            if (text.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;
                if (j == pattern.length()) {
                    matches.add(i - j); // full match found starting here
                    j = lps[j - 1];      // continue looking for further matches
                }
            } else if (j > 0) {
                j = lps[j - 1]; // fall back using the LPS table, don't restart i
            } else {
                i++;
            }
        }
        return matches;
    }

    /**
     * Builds the LPS array. lps[k] = length of the longest proper prefix of
     * pattern[0..k] that is also a suffix of pattern[0..k]. This is what lets
     * KMP skip ahead on a mismatch instead of re-scanning characters it already
     * matched.
     */
    private static int[] buildLpsArray(String pattern) {
        int[] lps = new int[pattern.length()];
        int len = 0; // length of the previous longest prefix-suffix
        int i = 1;
        lps[0] = 0;

        while (i < pattern.length()) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else if (len > 0) {
                len = lps[len - 1];
            } else {
                lps[i] = 0;
                i++;
            }
        }
        return lps;
    }

    public static void main(String[] args) throws IOException {
        String corpusDir = (args.length > 0) ? args[0] : "corpus";
        PatternSearch engine = new PatternSearch(corpusDir);

        String[] demoPatterns = {"Music", "Football", "Grade A"};

        for (String pattern : demoPatterns) {
            long start = System.nanoTime();
            List<Match> matches = engine.search(pattern);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            System.out.println("Pattern: \"" + pattern + "\" -> " + matches.size()
                    + " match(es) in " + elapsedMs + " ms");
            int shown = 0;
            for (Match m : matches) {
                System.out.println("  " + m);
                if (++shown >= 5) {
                    System.out.println("  ... and " + (matches.size() - shown) + " more.");
                    break;
                }
            }
            System.out.println();
        }
    }
}
