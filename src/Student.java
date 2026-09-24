public class Student {
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
