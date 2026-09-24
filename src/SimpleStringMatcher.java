/**
 * Basic string matching algorithm: case-insensitive substring match.
 */
public class SimpleStringMatcher implements StringMatcher {

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
        return t.contains(q);
    }
}
