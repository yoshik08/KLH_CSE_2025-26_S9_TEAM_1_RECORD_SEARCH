import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentRepository {

    private final List<Student> students = new ArrayList<>();
    private final Map<String, Student> indexById = new HashMap<>();
    private final StringMatcher matcher;

    public StudentRepository(StringMatcher matcher) {
        this.matcher = matcher;
    }

    public void add(Student student) {
        students.add(student);
        indexById.put(student.getStudentId().toLowerCase(), student);
    }

    public int size() {
        return students.size();
    }

    public Student getById(String studentId) {
        if (studentId == null) return null;
        return indexById.get(studentId.trim().toLowerCase());
    }

    public List<Student> searchByName(String query) {
        List<Student> results = new ArrayList<>();
        for (Student s : students) {
            if (matcher.matches(query, s.getName())) results.add(s);
        }
        return results;
    }

    public List<Student> searchByClass(String query) {
        List<Student> results = new ArrayList<>();
        for (Student s : students) {
            if (matcher.matches(query, s.getStudentClass())) results.add(s);
        }
        return results;
    }

    public List<Student> searchByExtracurricular(String query) {
        List<Student> results = new ArrayList<>();
        for (Student s : students) {
            if (matcher.matches(query, s.getExtracurricular())) results.add(s);
        }
        return results;
    }

    public List<Student> getAll() {
        return students;
    }
}
