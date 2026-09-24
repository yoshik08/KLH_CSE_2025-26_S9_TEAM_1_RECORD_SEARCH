import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Console entry point.
 *
 * Run:
 *   java -cp bin Main data/student_records.csv
 *
 * Field search (options 1-4) can run in Exact mode (SimpleStringMatcher, CO2-
 * adjacent baseline) or Fuzzy mode (FuzzyStringMatcher, edit distance / CO3).
 * Option 5 is the separate KMP pattern search over the text corpus (CO2).
 */
public class Main {

    public static void main(String[] args) throws IOException {
        String csvPath = (args.length > 0) ? args[0] : "data/student_records.csv";
        String corpusDir = "corpus";

        // Two repositories sharing the same data, differing only in matcher.
        // This is exactly the upgrade path StringMatcher was designed for.
        StudentRepository exactRepo = new StudentRepository(new SimpleStringMatcher());
        StudentRepository fuzzyRepo = new StudentRepository(new FuzzyStringMatcher(2));

        try {
            CsvLoader.load(csvPath, exactRepo);
            CsvLoader.load(csvPath, fuzzyRepo);
        } catch (IOException e) {
            System.out.println("Could not load corpus file: " + csvPath);
            System.out.println(e.getMessage());
            return;
        }
        System.out.println("Loaded " + exactRepo.size() + " student records from " + csvPath);

        CorpusBuilder.build(exactRepo, corpusDir);
        PatternSearch patternEngine = new PatternSearch(corpusDir);

        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        boolean fuzzyMode = false;

        while (running) {
            StudentRepository repository = fuzzyMode ? fuzzyRepo : exactRepo;

            System.out.println();
            System.out.println("=== Student Record Search [Mode: " + (fuzzyMode ? "FUZZY (edit distance)" : "EXACT") + "] ===");
            System.out.println("1. Search by Name");
            System.out.println("2. Search by Student ID (indexed lookup)");
            System.out.println("3. Search by Class");
            System.out.println("4. Search by Extracurricular");
            System.out.println("5. Pattern Search in Corpus (KMP)");
            System.out.println("6. Toggle Exact / Fuzzy search mode");
            System.out.println("7. Exit");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    System.out.print("Enter name (or part of it): ");
                    printResults(repository.searchByName(scanner.nextLine()));
                    break;
                case "2":
                    System.out.print("Enter Student ID: ");
                    Student s = repository.getById(scanner.nextLine());
                    System.out.println(s != null ? s : "No student found with that ID.");
                    break;
                case "3":
                    System.out.print("Enter class: ");
                    printResults(repository.searchByClass(scanner.nextLine()));
                    break;
                case "4":
                    System.out.print("Enter extracurricular activity: ");
                    printResults(repository.searchByExtracurricular(scanner.nextLine()));
                    break;
                case "5":
                    System.out.print("Enter pattern to search for in corpus: ");
                    String pattern = scanner.nextLine();
                    long start = System.nanoTime();
                    List<PatternSearch.Match> matches = patternEngine.search(pattern);
                    long elapsedMs = (System.nanoTime() - start) / 1_000_000;
                    if (matches.isEmpty()) {
                        System.out.println("No occurrences of \"" + pattern + "\" found in corpus.");
                    } else {
                        System.out.println("Found " + matches.size() + " occurrence(s) in " + elapsedMs + " ms:");
                        int shown = 0;
                        for (PatternSearch.Match m : matches) {
                            System.out.println("  " + m);
                            shown++;
                            if (shown >= 15 && matches.size() > 15) {
                                System.out.println("  ... and " + (matches.size() - shown) + " more.");
                                break;
                            }
                        }
                    }
                    break;
                case "6":
                    fuzzyMode = !fuzzyMode;
                    System.out.println("Switched to " + (fuzzyMode ? "FUZZY" : "EXACT") + " mode.");
                    break;
                case "7":
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option, try again.");
            }
        }

        scanner.close();
        System.out.println("Goodbye!");
    }

    private static void printResults(List<Student> results) {
        if (results.isEmpty()) {
            System.out.println("No matches found.");
            return;
        }
        System.out.println("Found " + results.size() + " match(es):");
        int shown = 0;
        for (Student s : results) {
            System.out.println("  " + s);
            if (++shown >= 15 && results.size() > 15) {
                System.out.println("  ... and " + (results.size() - shown) + " more.");
                break;
            }
        }
    }
}
