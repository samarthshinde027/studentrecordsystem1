package studentrecord;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import studentrecord.Dtos.Mark;

@Repository
public class MarksRepository {

    private static final RowMapper<Mark> MAPPER = (rs, i) -> new Mark(
            rs.getString("subject"), rs.getInt("marks"), rs.getInt("max_marks"));

    private final JdbcTemplate jdbc;

    public MarksRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Mark> list(String prn) {
        return jdbc.query("SELECT subject, marks, max_marks FROM marks WHERE prn = ? ORDER BY subject", MAPPER, prn);
    }

    public void upsert(String prn, Mark m) {
        jdbc.update("INSERT INTO marks (prn, subject, marks, max_marks) VALUES (?, ?, ?, ?) "
                + "ON CONFLICT (prn, subject) DO UPDATE SET marks = excluded.marks, max_marks = excluded.max_marks",
                prn, m.subject(), m.marks(), m.maxMarks());
    }

    public int delete(String prn, String subject) {
        return jdbc.update("DELETE FROM marks WHERE prn = ? AND subject = ?", prn, subject);
    }
}
