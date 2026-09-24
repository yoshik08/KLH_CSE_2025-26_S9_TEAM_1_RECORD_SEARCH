import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CorpusBuilder {

    public static void build(StudentRepository repository, String outputDir) throws IOException {
        Map<String, StringBuilder> byClass = new HashMap<>();

        for (Student s : repository.getAll()) {
            byClass
                .computeIfAbsent(s.getStudentClass(), k -> new StringBuilder())
                .append(s.toProfileText())
                .append(System.lineSeparator());
        }

        java.io.File dir = new java.io.File(outputDir);
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
