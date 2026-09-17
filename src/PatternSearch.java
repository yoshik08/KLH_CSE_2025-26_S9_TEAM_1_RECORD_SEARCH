import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Pattern search over the text corpus, using the Knuth-Morris-Pratt (KMP)
 * string matching algorithm. CO2.
 */
public class PatternSearch {

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

    public List<Match> search(String pattern) throws IOException {
        List<Match> results = new ArrayList<>();
        File dir = new File(corpusDir);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (files == null) {
            System.out.println("Warning: corpus folder \"" + corpusDir + "\" not found or empty.");
            return results;
        }

        java.util.Arrays.sort(files);

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

    private static List<Integer> kmpSearch(String text, String pattern) {
        List<Integer> matches = new ArrayList<>();
        if (text == null || pattern == null || pattern.isEmpty() || text.isEmpty()) {
            return matches;
        }

        int[] lps = buildLpsArray(pattern);
        int i = 0;
        int j = 0;

        while (i < text.length()) {
            if (text.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;
                if (j == pattern.length()) {
                    matches.add(i - j);
                    j = lps[j - 1];
                }
            } else if (j > 0) {
                j = lps[j - 1];
            } else {
                i++;
            }
        }
        return matches;
    }

    private static int[] buildLpsArray(String pattern) {
        int[] lps = new int[pattern.length()];
        int len = 0;
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
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Pattern Search (KMP) over corpus: \"" + corpusDir + "\" ===");
        System.out.println("Type a pattern to search for, or type 'exit' to quit.");

        while (true) {
            System.out.print("\nEnter pattern: ");
            String pattern = scanner.nextLine().trim();

            if (pattern.equalsIgnoreCase("exit")) break;
            if (pattern.isEmpty()) {
                System.out.println("Pattern cannot be empty. Try again.");
                continue;
            }

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
        }

        scanner.close();
        System.out.println("Goodbye!");
    }
}
