import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CsvLoader {

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
