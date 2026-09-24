import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;

/**
 * Intelligent Student Record Search System — DSA-3 Final Project.
 *
 * Combines three independently-built and independently-tested algorithms
 * into one runnable console application:
 *
 *   CO2 — KMP (Knuth-Morris-Pratt) pattern search over a generated text
 *         corpus of student profiles.                         [PatternSearch]
 *   CO3 — Edit Distance (Levenshtein, via DP), used for typo-tolerant
 *         ("fuzzy") field search, toggled at runtime.       [FuzzyStringMatcher]
 *   CO4 — Maximum Flow (Ford-Fulkerson / Edmonds-Karp), used to solve a
 *         skill-based student-to-job assignment problem.      [SkillJobMatcher]
 *
 * Everything else (Student, StudentRepository, CsvLoader, CorpusBuilder,
 * StringMatcher/SimpleStringMatcher) is the supporting data layer these
 * three algorithms run on top of.
 *
 * Run:
 *   javac StudentDSASystem.java
 *   java StudentDSASystem data/student_records.csv
 */
public class StudentDSASystem {

    public static void main(String[] args) throws IOException {
        String csvPath = (args.length > 0) ? args[0] : "data/student_records.csv";
        String corpusDir = "corpus";

        // Two repositories sharing the same data, differing only in which
        // StringMatcher they use — the upgrade path StringMatcher was
        // designed for from the start.
        StudentRepository exactRepo = new StudentRepository(new SimpleStringMatcher());
        StudentRepository fuzzyRepo = new StudentRepository(new FuzzyStringMatcher(2));

        try {
            CsvLoader.load(csvPath, exactRepo);
            CsvLoader.load(csvPath, fuzzyRepo);
        } catch (IOException e) {
            System.out.println("Could not load data file: " + csvPath);
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
            System.out.println("=== Student DSA System [Mode: " + (fuzzyMode ? "FUZZY (edit distance)" : "EXACT") + "] ===");
            System.out.println("1. Search by Name");
            System.out.println("2. Search by Student ID (indexed lookup)");
            System.out.println("3. Search by Class");
            System.out.println("4. Search by Extracurricular");
            System.out.println("5. Toggle Exact / Fuzzy search mode");
            System.out.println("6. Pattern Search in Corpus (KMP)");
            System.out.println("7. Skill-Job Matching Demo (Max Flow)");
            System.out.println("8. Exit");
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
                    fuzzyMode = !fuzzyMode;
                    System.out.println("Switched to " + (fuzzyMode ? "FUZZY" : "EXACT") + " mode.");
                    break;
                case "6":
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
                case "7":
                    SkillJobMatcher.runDemo();
                    break;
                case "8":
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

// =============================================================================
// Data layer
// =============================================================================

/**
 * Represents a single student record loaded from the CSV.
 * Plain data holder — no search logic lives here on purpose.
 */
class Student {
    private final String studentId;
    private final String name;
    private final int age;
    private final String gender;
    private final String studentClass;
    private final int mathScore;
    private final int scienceScore;
    private final int englishScore;
    private final int historyScore;
    private final int computerScore;
    private final double attendancePercent;
    private final String extracurricular;
    private final int total;
    private final String grade;

    public Student(String studentId, String name, int age, String gender, String studentClass,
                   int mathScore, int scienceScore, int englishScore, int historyScore, int computerScore,
                   double attendancePercent, String extracurricular, int total, String grade) {
        this.studentId = studentId;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.studentClass = studentClass;
        this.mathScore = mathScore;
        this.scienceScore = scienceScore;
        this.englishScore = englishScore;
        this.historyScore = historyScore;
        this.computerScore = computerScore;
        this.attendancePercent = attendancePercent;
        this.extracurricular = extracurricular;
        this.total = total;
        this.grade = grade;
    }

    public String getStudentId() { return studentId; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public String getStudentClass() { return studentClass; }
    public int getMathScore() { return mathScore; }
    public int getScienceScore() { return scienceScore; }
    public int getEnglishScore() { return englishScore; }
    public int getHistoryScore() { return historyScore; }
    public int getComputerScore() { return computerScore; }
    public double getAttendancePercent() { return attendancePercent; }
    public String getExtracurricular() { return extracurricular; }
    public int getTotal() { return total; }
    public String getGrade() { return grade; }

    /** One flowing sentence per student — used to build the text corpus documents. */
    public String toProfileText() {
        return String.format(
            "Student %s named %s, age %d, gender %s, is in Class %s. " +
            "Scored %d in Math, %d in Science, %d in English, %d in History, and %d in Computer, " +
            "totaling %d marks with Grade %s. Attendance stands at %.2f percent. " +
            "Participates in %s as an extracurricular activity.",
            studentId, name, age, gender, studentClass,
            mathScore, scienceScore, englishScore, historyScore, computerScore,
            total, grade, attendancePercent, extracurricular
        );
    }

    @Override
    public String toString() {
        return String.format(
            "%-8s %-20s Class:%-3s Age:%-3d Total:%-4d Grade:%-2s Attendance:%.1f%%  Extracurricular:%s",
            studentId, name, studentClass, age, total, grade, attendancePercent, extracurricular
        );
    }
}

/**
 * Contract for any string matching algorithm used by field-based search.
 * Two implementations: SimpleStringMatcher (exact) and FuzzyStringMatcher
 * (edit distance). Swapping between them never requires changing
 * StudentRepository — that's the whole point of the interface.
 */
interface StringMatcher {
    boolean matches(String query, String target);
}

/** Basic string matching algorithm: case-insensitive substring match. */
class SimpleStringMatcher implements StringMatcher {
    @Override
    public boolean matches(String query, String target) {
        if (query == null || target == null) return false;
        String q = query.trim().toLowerCase();
        String t = target.trim().toLowerCase();
        if (q.isEmpty()) return false;
        return t.contains(q);
    }
}

/**
 * Fuzzy string matching algorithm using Edit Distance (Levenshtein Distance),
 * computed with dynamic programming. CO3.
 */
class FuzzyStringMatcher implements StringMatcher {

    private final int threshold;

    public FuzzyStringMatcher() { this(2); }

    public FuzzyStringMatcher(int threshold) { this.threshold = threshold; }

    @Override
    public boolean matches(String query, String target) {
        if (query == null || target == null) return false;
        String q = query.trim().toLowerCase();
        String t = target.trim().toLowerCase();
        if (q.isEmpty()) return false;

        if (t.contains(q)) return true;

        for (String word : t.split("\\s+")) {
            if (editDistance(q, word) <= threshold) return true;
        }
        return false;
    }

    /**
     * Levenshtein edit distance via dynamic programming (bottom-up).
     * dp[i][j] = edit distance between the first i chars of a and first j chars of b.
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

/**
 * Central repository of student records.
 *  - ArrayList<Student>        : the master list, used for field search
 *  - HashMap<String, Student>  : indexed by StudentID, for instant O(1) lookup
 */
class StudentRepository {
    private final List<Student> students = new ArrayList<>();
    private final Map<String, Student> indexById = new HashMap<>();
    private final StringMatcher matcher;

    public StudentRepository(StringMatcher matcher) { this.matcher = matcher; }

    public void add(Student student) {
        students.add(student);
        indexById.put(student.getStudentId().toLowerCase(), student);
    }

    public int size() { return students.size(); }

    public Student getById(String studentId) {
        if (studentId == null) return null;
        return indexById.get(studentId.trim().toLowerCase());
    }

    public List<Student> searchByName(String query) {
        List<Student> results = new ArrayList<>();
        for (Student s : students) if (matcher.matches(query, s.getName())) results.add(s);
        return results;
    }

    public List<Student> searchByClass(String query) {
        List<Student> results = new ArrayList<>();
        for (Student s : students) if (matcher.matches(query, s.getStudentClass())) results.add(s);
        return results;
    }

    public List<Student> searchByExtracurricular(String query) {
        List<Student> results = new ArrayList<>();
        for (Student s : students) if (matcher.matches(query, s.getExtracurricular())) results.add(s);
        return results;
    }

    public List<Student> getAll() { return students; }
}

/** Loads student records from the CSV file into a StudentRepository. */
class CsvLoader {
    public static void load(String filePath, StudentRepository repository) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine(); // header row, skipped
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;

                String[] fields = line.split(",", -1);
                if (fields.length < 14) {
                    System.out.println("Skipping malformed row " + lineNumber + ": " + line);
                    continue;
                }

                try {
                    Student student = new Student(
                        fields[0].trim(), fields[1].trim(), Integer.parseInt(fields[2].trim()),
                        fields[3].trim(), fields[4].trim(),
                        Integer.parseInt(fields[5].trim()), Integer.parseInt(fields[6].trim()),
                        Integer.parseInt(fields[7].trim()), Integer.parseInt(fields[8].trim()),
                        Integer.parseInt(fields[9].trim()), Double.parseDouble(fields[10].trim()),
                        fields[11].trim(), Integer.parseInt(fields[12].trim()), fields[13].trim()
                    );
                    repository.add(student);
                } catch (NumberFormatException e) {
                    System.out.println("Skipping row " + lineNumber + " (bad number): " + line);
                }
            }
        }
    }
}

/**
 * Builds the text corpus for pattern search out of the loaded student data.
 * Groups students by Class and writes one flowing text document per class.
 */
class CorpusBuilder {
    public static void build(StudentRepository repository, String outputDir) throws IOException {
        Map<String, StringBuilder> byClass = new HashMap<>();

        for (Student s : repository.getAll()) {
            byClass
                .computeIfAbsent(s.getStudentClass(), k -> new StringBuilder())
                .append(s.toProfileText())
                .append(System.lineSeparator());
        }

        File dir = new File(outputDir);
        if (!dir.exists()) dir.mkdirs();

        for (Map.Entry<String, StringBuilder> entry : byClass.entrySet()) {
            String fileName = outputDir + "/class_" + entry.getKey() + ".txt";
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
                writer.write(entry.getValue().toString());
            }
        }

        System.out.println("Corpus built: " + byClass.size() + " documents written to " + outputDir);
    }
}

// =============================================================================
// CO2 — KMP Pattern Search
// =============================================================================

/**
 * Pattern search over the text corpus, using the Knuth-Morris-Pratt (KMP)
 * string matching algorithm.
 */
class PatternSearch {

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

    public PatternSearch(String corpusDir) { this.corpusDir = corpusDir; }

    public List<Match> search(String pattern) throws IOException {
        List<Match> results = new ArrayList<>();
        File dir = new File(corpusDir);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (files == null) {
            System.out.println("Warning: corpus folder \"" + corpusDir + "\" not found or empty.");
            return results;
        }

        Arrays.sort(files);

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
        if (text == null || pattern == null || pattern.isEmpty() || text.isEmpty()) return matches;

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
}

// =============================================================================
// CO4 — Maximum Flow (Skill-Job Matching)
// =============================================================================

/**
 * Skill-based Job Matching using Maximum Flow (Ford-Fulkerson, Edmonds-Karp
 * variant using BFS to find augmenting paths).
 *
 * Source --1--> [Student] --1--> [Job] --openings--> Sink
 * A Student->Job edge exists only if the student's skills cover every skill
 * the job requires. Max flow = maximum number of students placeable into
 * jobs without exceeding any job's opening count.
 */
class SkillJobMatcher {

    static class StudentProfile {
        String id;
        String name;
        Set<String> skills;

        StudentProfile(String id, String name, String... skills) {
            this.id = id;
            this.name = name;
            this.skills = new HashSet<>(Arrays.asList(skills));
        }
    }

    static class JobPosting {
        String id;
        String title;
        Set<String> requiredSkills;
        int openings;

        JobPosting(String id, String title, int openings, String... requiredSkills) {
            this.id = id;
            this.title = title;
            this.openings = openings;
            this.requiredSkills = new HashSet<>(Arrays.asList(requiredSkills));
        }
    }

    static List<StudentProfile> buildStudents() {
        List<StudentProfile> students = new ArrayList<>();
        students.add(new StudentProfile("S01", "Aarav",   "JAVA"));
        students.add(new StudentProfile("S02", "Meera",   "JAVA", "REACT"));
        students.add(new StudentProfile("S03", "Kabir",   "PYTHON", "SQL"));
        students.add(new StudentProfile("S04", "Diya",    "REACT", "HTML"));
        students.add(new StudentProfile("S05", "Vihaan",  "JAVA", "SQL"));
        students.add(new StudentProfile("S06", "Ananya",  "PYTHON", "ML"));
        students.add(new StudentProfile("S07", "Reyansh", "JAVA", "REACT", "SQL"));
        students.add(new StudentProfile("S08", "Ishita",  "HTML", "REACT"));
        students.add(new StudentProfile("S09", "Aditya",  "PYTHON", "CLOUD"));
        students.add(new StudentProfile("S10", "Saanvi",  "JAVA"));
        students.add(new StudentProfile("S11", "Arjun",   "NODE", "JAVA"));
        students.add(new StudentProfile("S12", "Priya",   "SQL"));
        students.add(new StudentProfile("S13", "Vivaan",  "PYTHON", "SQL", "CLOUD"));
        students.add(new StudentProfile("S14", "Anika",   "REACT"));
        students.add(new StudentProfile("S15", "Reyan",   "JAVA", "CLOUD"));
        students.add(new StudentProfile("S16", "Myra",    "ML", "PYTHON"));
        students.add(new StudentProfile("S17", "Kian",    "NODE", "REACT"));
        students.add(new StudentProfile("S18", "Aadhya",  "HTML"));
        students.add(new StudentProfile("S19", "Arnav",   "JAVA", "REACT", "CLOUD"));
        students.add(new StudentProfile("S20", "Navya",   "SQL", "PYTHON"));
        students.add(new StudentProfile("S21", "Dhruv",   "CLOUD"));
        students.add(new StudentProfile("S22", "Pihu",    "ML"));
        students.add(new StudentProfile("S23", "Yuvan",   "JAVA", "NODE", "SQL"));
        students.add(new StudentProfile("S24", "Riya",    "REACT", "HTML", "JAVA"));
        return students;
    }

    static List<JobPosting> buildJobs() {
        List<JobPosting> jobs = new ArrayList<>();
        jobs.add(new JobPosting("J1", "Backend Developer",  2, "JAVA", "SQL"));
        jobs.add(new JobPosting("J2", "Frontend Developer", 2, "REACT", "HTML"));
        jobs.add(new JobPosting("J3", "Full Stack Developer", 1, "JAVA", "REACT"));
        jobs.add(new JobPosting("J4", "Data Analyst",       2, "PYTHON", "SQL"));
        jobs.add(new JobPosting("J5", "ML Engineer",        1, "PYTHON", "ML"));
        jobs.add(new JobPosting("J6", "Cloud Engineer",     1, "CLOUD", "PYTHON"));
        jobs.add(new JobPosting("J7", "Java Developer",     3, "JAVA"));
        jobs.add(new JobPosting("J8", "Node.js Developer",  1, "NODE", "JAVA"));
        return jobs;
    }

    private static boolean isEligible(StudentProfile student, JobPosting job) {
        return student.skills.containsAll(job.requiredSkills);
    }

    static int runMaxFlow(List<StudentProfile> students, List<JobPosting> jobs,
                           List<String> assignmentsOut) {

        int n = students.size();
        int m = jobs.size();

        int source = 0;
        int sink = n + m + 1;
        int totalNodes = n + m + 2;

        int[][] capacity = new int[totalNodes][totalNodes];
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < totalNodes; i++) adj.add(new ArrayList<>());

        for (int i = 0; i < n; i++) {
            int studentNode = 1 + i;
            capacity[source][studentNode] = 1;
            adj.get(source).add(studentNode);
            adj.get(studentNode).add(source);
        }

        for (int i = 0; i < n; i++) {
            int studentNode = 1 + i;
            for (int j = 0; j < m; j++) {
                if (isEligible(students.get(i), jobs.get(j))) {
                    int jobNode = 1 + n + j;
                    capacity[studentNode][jobNode] = 1;
                    adj.get(studentNode).add(jobNode);
                    adj.get(jobNode).add(studentNode);
                }
            }
        }

        for (int j = 0; j < m; j++) {
            int jobNode = 1 + n + j;
            capacity[jobNode][sink] = jobs.get(j).openings;
            adj.get(jobNode).add(sink);
            adj.get(sink).add(jobNode);
        }

        int maxFlow = 0;
        int[] parent = new int[totalNodes];

        while (bfsFindPath(capacity, adj, source, sink, parent)) {
            int pathFlow = Integer.MAX_VALUE;
            for (int v = sink; v != source; v = parent[v]) {
                int u = parent[v];
                pathFlow = Math.min(pathFlow, capacity[u][v]);
            }
            for (int v = sink; v != source; v = parent[v]) {
                int u = parent[v];
                capacity[u][v] -= pathFlow;
                capacity[v][u] += pathFlow;
            }
            maxFlow += pathFlow;
        }

        for (int i = 0; i < n; i++) {
            int studentNode = 1 + i;
            for (int j = 0; j < m; j++) {
                if (isEligible(students.get(i), jobs.get(j))) {
                    int jobNode = 1 + n + j;
                    if (capacity[jobNode][studentNode] > 0) {
                        assignmentsOut.add(students.get(i).id + " (" + students.get(i).name + ") -> "
                                + jobs.get(j).id + " (" + jobs.get(j).title + ")");
                    }
                }
            }
        }

        return maxFlow;
    }

    private static boolean bfsFindPath(int[][] capacity, List<List<Integer>> adj,
                                        int source, int sink, int[] parent) {
        Arrays.fill(parent, -1);
        parent[source] = source;
        Queue<Integer> queue = new LinkedList<>();
        queue.add(source);

        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int v : adj.get(u)) {
                if (parent[v] == -1 && capacity[u][v] > 0) {
                    parent[v] = u;
                    if (v == sink) return true;
                    queue.add(v);
                }
            }
        }
        return false;
    }

    /** Runs the full skill-job matching demo: prints input data, result, and assignments. */
    static void runDemo() {
        List<StudentProfile> students = buildStudents();
        List<JobPosting> jobs = buildJobs();

        System.out.println("\n=== Students (" + students.size() + ") ===");
        for (StudentProfile s : students) {
            System.out.println("  " + s.id + " " + s.name + " - skills: " + s.skills);
        }

        System.out.println("\n=== Jobs (" + jobs.size() + ") ===");
        int totalOpenings = 0;
        for (JobPosting j : jobs) {
            System.out.println("  " + j.id + " " + j.title + " - requires: " + j.requiredSkills
                    + " - openings: " + j.openings);
            totalOpenings += j.openings;
        }
        System.out.println("Total openings across all jobs: " + totalOpenings);

        List<String> assignments = new ArrayList<>();
        int maxFlow = runMaxFlow(students, jobs, assignments);

        System.out.println("\n=== Max Flow Result ===");
        System.out.println("Maximum number of students that can be placed: " + maxFlow
                + " out of " + students.size() + " students, " + totalOpenings + " total openings.");

        System.out.println("\nAssignments:");
        for (String a : assignments) {
            System.out.println("  " + a);
        }

        Set<String> matchedIds = new HashSet<>();
        for (String a : assignments) {
            matchedIds.add(a.substring(0, a.indexOf(' ')));
        }
        System.out.println("\nUnmatched students:");
        for (StudentProfile s : students) {
            if (!matchedIds.contains(s.id)) {
                System.out.println("  " + s.id + " " + s.name + " - skills: " + s.skills);
            }
        }
    }
}
