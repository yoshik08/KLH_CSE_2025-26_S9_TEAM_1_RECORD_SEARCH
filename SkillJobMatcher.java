import java.util.*;

/**
 * Skill-based Job Matching using Maximum Flow (Ford-Fulkerson, Edmonds-Karp
 * variant using BFS to find augmenting paths). CO4.
 *
 * -------------------------------------------------------------------------
 * THE PROBLEM
 * We have a set of students, each with a set of skills (e.g. Java, React).
 * We have a set of job openings, each requiring a specific set of skills
 * and having a limited number of openings (capacity).
 *
 * A student is only eligible for a job if their skillset covers every skill
 * the job requires (having MORE skills than needed is fine — a Java+React
 * student can still take a Java-only job).
 *
 * We want the MAXIMUM number of students that can be placed into jobs,
 * such that:
 *   - each student is placed into at most one job
 *   - no job exceeds its number of openings
 *
 * This is exactly a bipartite matching problem with capacities — one of the
 * classic real-world applications of max flow.
 *
 * -------------------------------------------------------------------------
 * HOW IT'S MODELED AS A FLOW NETWORK
 *
 *   Source --1--> [Student] --1--> [Job] --openings--> Sink
 *
 *   - Source -> each Student: capacity 1 (a student can be placed once)
 *   - Student -> Job: capacity 1, edge exists ONLY if the student is
 *     eligible for that job (eligibility is decided before building the
 *     graph — it's a simple "does student's skill set contain all of the
 *     job's required skills" check, not something the flow algorithm
 *     itself has to figure out)
 *   - Job -> Sink: capacity = number of openings for that job
 *
 * The maximum flow from Source to Sink equals the maximum number of
 * students that can be placed. Looking at which Student->Job edges
 * actually carry flow afterwards tells us exactly who got placed where.
 * -------------------------------------------------------------------------
 */
public class SkillJobMatcher {

    // ------------------------------------------------------------------
    // Data model
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Sample data (hardcoded so the demo is self-contained and reproducible
    // — no CSV, no file paths, nothing that can go missing before a demo)
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Max Flow (Ford-Fulkerson, Edmonds-Karp: BFS to find augmenting paths)
    // ------------------------------------------------------------------

    /**
     * Builds the flow network and runs Edmonds-Karp.
     * @return total max flow (= number of students successfully placed)
     *         and, via the assignments list, who was placed where.
     */
    static int runMaxFlow(List<StudentProfile> students, List<JobPosting> jobs,
                           List<String> assignmentsOut) {

        int n = students.size();
        int m = jobs.size();

        // Node numbering: 0 = source, 1..n = students, n+1..n+m = jobs, n+m+1 = sink
        int source = 0;
        int sink = n + m + 1;
        int totalNodes = n + m + 2;

        int[][] capacity = new int[totalNodes][totalNodes];
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < totalNodes; i++) adj.add(new ArrayList<>());

        // Source -> Student (capacity 1 each)
        for (int i = 0; i < n; i++) {
            int studentNode = 1 + i;
            capacity[source][studentNode] = 1;
            adj.get(source).add(studentNode);
            adj.get(studentNode).add(source); // reverse edge for residual graph
        }

        // Student -> Job (capacity 1, only if eligible)
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

        // Job -> Sink (capacity = openings)
        for (int j = 0; j < m; j++) {
            int jobNode = 1 + n + j;
            capacity[jobNode][sink] = jobs.get(j).openings;
            adj.get(jobNode).add(sink);
            adj.get(sink).add(jobNode);
        }

        int maxFlow = 0;
        int[] parent = new int[totalNodes];

        // Repeatedly find an augmenting path via BFS, and push 1 unit of
        // flow along it (bottleneck is always 1 here, since every
        // Source->Student and Student->Job edge has capacity 1).
        while (bfsFindPath(capacity, adj, source, sink, parent)) {
            // Find the bottleneck capacity along this path
            int pathFlow = Integer.MAX_VALUE;
            for (int v = sink; v != source; v = parent[v]) {
                int u = parent[v];
                pathFlow = Math.min(pathFlow, capacity[u][v]);
            }

            // Update residual capacities along the path
            for (int v = sink; v != source; v = parent[v]) {
                int u = parent[v];
                capacity[u][v] -= pathFlow; // use up forward capacity
                capacity[v][u] += pathFlow; // open up backward capacity
            }

            maxFlow += pathFlow;
        }

        // Read off the final assignments: for each student, check which
        // job edge got used up (original capacity 1, now 0 forward /
        // 1 backward means flow passed through it).
        for (int i = 0; i < n; i++) {
            int studentNode = 1 + i;
            for (int j = 0; j < m; j++) {
                if (isEligible(students.get(i), jobs.get(j))) {
                    int jobNode = 1 + n + j;
                    // capacity[jobNode][studentNode] > 0 means flow was pushed
                    // student -> job (the reverse residual edge got filled)
                    if (capacity[jobNode][studentNode] > 0) {
                        assignmentsOut.add(students.get(i).id + " (" + students.get(i).name + ") -> "
                                + jobs.get(j).id + " (" + jobs.get(j).title + ")");
                    }
                }
            }
        }

        return maxFlow;
    }

    /** Standard BFS over the residual graph, used to find one augmenting path. */
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

    // ------------------------------------------------------------------
    // Demo entry point
    // ------------------------------------------------------------------

    public static void main(String[] args) {
        List<StudentProfile> students = buildStudents();
        List<JobPosting> jobs = buildJobs();

        System.out.println("=== Students (" + students.size() + ") ===");
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
 
 
