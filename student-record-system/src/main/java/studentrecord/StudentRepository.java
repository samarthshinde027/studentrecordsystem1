package studentrecord;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import studentrecord.Dtos.PageResult;

/** Data access layer for students and photos (parameterized queries only). */
@Repository
public class StudentRepository {

    private static final RowMapper<Student> MAPPER = (rs, i) -> new Student(
            rs.getString("prn"),
            rs.getString("name"),
            rs.getString("course"),
            rs.getInt("year"),
            rs.getString("phone"));

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "prn", "prn", "name", "name", "course", "course", "year", "year", "phone", "phone");

    private final JdbcTemplate jdbc;

    public StudentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PageResult<Student> search(String q, String course, Integer year,
                                      String sort, String dir, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            where.append(" AND (LOWER(prn) LIKE ? OR LOWER(name) LIKE ?)");
            String like = "%" + q.trim().toLowerCase() + "%";
            args.add(like);
            args.add(like);
        }
        if (course != null && !course.isBlank()) {
            where.append(" AND course = ?");
            args.add(course);
        }
        if (year != null) {
            where.append(" AND year = ?");
            args.add(year);
        }

        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM students" + where, Long.class, args.toArray());
        long total = count == null ? 0L : count;

        String column = SORT_COLUMNS.getOrDefault(sort, "name");
        String direction = "desc".equalsIgnoreCase(dir) ? "DESC" : "ASC";
        String sql = "SELECT * FROM students" + where
                + " ORDER BY " + column + " " + direction + ", prn ASC LIMIT ? OFFSET ?";
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add(page * size);
        List<Student> items = jdbc.query(sql, MAPPER, pageArgs.toArray());
        return new PageResult<>(items, total, page, size);
    }

    public List<Student> findAll() {
        return jdbc.query("SELECT * FROM students ORDER BY name", MAPPER);
    }

    public List<String> courses() {
        return jdbc.queryForList("SELECT DISTINCT course FROM students ORDER BY course", String.class);
    }

    public long count() {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM students", Long.class);
        return c == null ? 0L : c;
    }

    public Map<String, Long> byCourse() {
        return groupCount("SELECT course, COUNT(*) FROM students GROUP BY course ORDER BY course");
    }

    public Map<String, Long> byYear() {
        return groupCount("SELECT year, COUNT(*) FROM students GROUP BY year ORDER BY year");
    }

    private Map<String, Long> groupCount(String sql) {
        ResultSetExtractor<Map<String, Long>> extractor = rs -> {
            Map<String, Long> map = new LinkedHashMap<>();
            while (rs.next()) {
                map.put(rs.getString(1), rs.getLong(2));
            }
            return map;
        };
        return jdbc.query(sql, extractor);
    }

    public Optional<Student> findByPrn(String prn) {
        return jdbc.query("SELECT * FROM students WHERE prn = ?", MAPPER, prn)
                .stream().findFirst();
    }

    public void insert(Student s) {
        jdbc.update(
                "INSERT INTO students (prn, name, course, year, phone) VALUES (?, ?, ?, ?, ?)",
                s.prn(), s.name(), s.course(), s.year(), s.phone());
    }

    public int update(Student s) {
        return jdbc.update(
                "UPDATE students SET name = ?, course = ?, year = ?, phone = ? WHERE prn = ?",
                s.name(), s.course(), s.year(), s.phone(), s.prn());
    }

    /** Deletes the student and all related rows. Returns 0 if the PRN was not found. */
    @Transactional
    public int delete(String prn) {
        jdbc.update("DELETE FROM attendance WHERE prn = ?", prn);
        jdbc.update("DELETE FROM marks WHERE prn = ?", prn);
        jdbc.update("DELETE FROM student_photos WHERE prn = ?", prn);
        return jdbc.update("DELETE FROM students WHERE prn = ?", prn);
    }

    public Optional<String> findPhoto(String prn) {
        return jdbc.query("SELECT photo FROM student_photos WHERE prn = ?",
                (rs, i) -> rs.getString(1), prn).stream().findFirst();
    }

    public void savePhoto(String prn, String photo) {
        jdbc.update("INSERT INTO student_photos (prn, photo) VALUES (?, ?) "
                + "ON CONFLICT (prn) DO UPDATE SET photo = excluded.photo", prn, photo);
    }

    public void deletePhoto(String prn) {
        jdbc.update("DELETE FROM student_photos WHERE prn = ?", prn);
    }
}
