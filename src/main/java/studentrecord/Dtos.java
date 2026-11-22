package studentrecord;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Small data-transfer objects used by the REST API. */
public final class Dtos {
    private Dtos() {}

    public record PageResult<T>(List<T> items, long total, int page, int size) {}

    public record LoginRequest(String username, String password) {}

    public record LoginResponse(String token, String username) {}

    public record Mark(
            @NotBlank @Size(max = 60) String subject,
            @NotNull @Min(0) Integer marks,
            @NotNull @Min(1) Integer maxMarks) {}

    public record ReportCard(
            Student student,
            List<Mark> marks,
            int totalMarks,
            int totalMax,
            double percentage,
            String grade,
            int attendancePresent,
            int attendanceTotal,
            double attendancePercent) {}

    public record AttendanceRow(String prn, String name, String status, int present, int total) {}

    public record AttendanceEntry(String prn, String status) {}

    public record AttendanceRequest(String date, List<AttendanceEntry> records) {}

    public record PhotoRequest(String photo) {}

    public record ImportResult(int added, int skipped, List<String> errors) {}

    public record Stats(
            long total,
            Map<String, Long> byCourse,
            Map<String, Long> byYear,
            double averageAttendance,
            long attendanceRecords) {}
}
