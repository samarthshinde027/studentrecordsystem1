package studentrecord;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import studentrecord.Dtos.AttendanceEntry;
import studentrecord.Dtos.AttendanceRow;

@Repository
public class AttendanceRepository {

    private static final RowMapper<AttendanceRow> MAPPER = (rs, i) -> new AttendanceRow(
            rs.getString("prn"),
            rs.getString("name"),
            rs.getString("status"),
            rs.getInt("present"),
            rs.getInt("total"));

    private final JdbcTemplate jdbc;

    public AttendanceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every student with their status on the given date (null = not marked) and overall counts. */
    public List<AttendanceRow> forDate(String date) {
        String sql = "SELECT s.prn AS prn, s.name AS name, a.status AS status, "
                + "(SELECT COUNT(*) FROM attendance x WHERE x.prn = s.prn AND x.status = 'PRESENT') AS present, "
                + "(SELECT COUNT(*) FROM attendance x WHERE x.prn = s.prn) AS total "
                + "FROM students s LEFT JOIN attendance a ON a.prn = s.prn AND a.att_date = ? "
                + "ORDER BY s.name";
        return jdbc.query(sql, MAPPER, date);
    }

    @Transactional
    public void saveAll(String date, List<AttendanceEntry> entries) {
        for (AttendanceEntry e : entries) {
            jdbc.update("INSERT INTO attendance (prn, att_date, status) VALUES (?, ?, ?) "
                    + "ON CONFLICT (prn, att_date) DO UPDATE SET status = excluded.status",
                    e.prn(), date, e.status());
        }
    }

    public int[] counts(String prn) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM attendance WHERE prn = ?", Integer.class, prn);
        Integer present = jdbc.queryForObject(
                "SELECT COUNT(*) FROM attendance WHERE prn = ? AND status = 'PRESENT'", Integer.class, prn);
        return new int[] { present == null ? 0 : present, total == null ? 0 : total };
    }

    public long totalRecords() {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM attendance", Long.class);
        return c == null ? 0L : c;
    }

    public long presentRecords() {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM attendance WHERE status = 'PRESENT'", Long.class);
        return c == null ? 0L : c;
    }
}
