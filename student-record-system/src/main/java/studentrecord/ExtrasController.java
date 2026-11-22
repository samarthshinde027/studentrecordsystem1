package studentrecord;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import studentrecord.Dtos.Mark;
import studentrecord.Dtos.PhotoRequest;
import studentrecord.Dtos.ReportCard;
import studentrecord.Dtos.Stats;

/** Dashboard stats, CSV export, report cards (marks) and photos. */
@RestController
@RequestMapping("/api")
public class ExtrasController {

    private final StudentRepository students;
    private final MarksRepository marks;
    private final AttendanceRepository attendance;

    public ExtrasController(StudentRepository students, MarksRepository marks, AttendanceRepository attendance) {
        this.students = students;
        this.marks = marks;
        this.attendance = attendance;
    }

    @GetMapping("/stats")
    public Stats stats() {
        long total = attendance.totalRecords();
        double avg = total == 0 ? 0 : Math.round(attendance.presentRecords() * 1000.0 / total) / 10.0;
        return new Stats(students.count(), students.byCourse(), students.byYear(), avg, total);
    }

    @GetMapping("/export/students.csv")
    public ResponseEntity<String> exportCsv() {
        StringBuilder sb = new StringBuilder("\uFEFFprn,name,course,year,phone\r\n");
        for (Student s : students.findAll()) {
            sb.append(csv(s.prn())).append(',')
              .append(csv(s.name())).append(',')
              .append(csv(s.course())).append(',')
              .append(s.year()).append(',')
              .append(csv(s.phone())).append("\r\n");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"students.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(sb.toString());
    }

    @GetMapping("/students/{prn}/report")
    public ReportCard report(@PathVariable String prn) {
        Student student = requireStudent(prn);
        List<Mark> list = marks.list(prn);
        int total = list.stream().mapToInt(Mark::marks).sum();
        int max = list.stream().mapToInt(Mark::maxMarks).sum();
        double pct = max == 0 ? 0 : Math.round(total * 10000.0 / max) / 100.0;
        int[] att = attendance.counts(prn);
        double attPct = att[1] == 0 ? 0 : Math.round(att[0] * 1000.0 / att[1]) / 10.0;
        return new ReportCard(student, list, total, max, pct, list.isEmpty() ? "-" : grade(pct),
                att[0], att[1], attPct);
    }

    @PostMapping("/students/{prn}/marks")
    public List<Mark> addMark(@PathVariable String prn, @Valid @RequestBody Mark body) {
        requireStudent(prn);
        Mark m = new Mark(body.subject().trim(), body.marks(), body.maxMarks());
        if (m.subject().isEmpty()) {
            throw bad("Subject is required");
        }
        if (m.marks() > m.maxMarks()) {
            throw bad("Marks cannot be more than maximum marks");
        }
        marks.upsert(prn, m);
        return marks.list(prn);
    }

    @DeleteMapping("/students/{prn}/marks")
    public List<Mark> deleteMark(@PathVariable String prn, @RequestParam String subject) {
        requireStudent(prn);
        marks.delete(prn, subject);
        return marks.list(prn);
    }

    @GetMapping("/students/{prn}/photo")
    public PhotoRequest getPhoto(@PathVariable String prn) {
        requireStudent(prn);
        return new PhotoRequest(students.findPhoto(prn).orElse(null));
    }

    @PutMapping("/students/{prn}/photo")
    public PhotoRequest putPhoto(@PathVariable String prn, @RequestBody PhotoRequest body) {
        requireStudent(prn);
        checkPhoto(body.photo());
        students.savePhoto(prn, body.photo());
        return body;
    }

    @DeleteMapping("/students/{prn}/photo")
    public PhotoRequest deletePhoto(@PathVariable String prn) {
        requireStudent(prn);
        students.deletePhoto(prn);
        return new PhotoRequest(null);
    }

    private Student requireStudent(String prn) {
        return students.findByPrn(prn).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static void checkPhoto(String p) {
        if (p == null || p.length() > 400_000) {
            throw bad("Photo is missing or too large");
        }
        String prefix;
        if (p.startsWith("data:image/jpeg;base64,")) {
            prefix = "data:image/jpeg;base64,";
        } else if (p.startsWith("data:image/png;base64,")) {
            prefix = "data:image/png;base64,";
        } else {
            throw bad("Photo must be a JPEG or PNG image");
        }
        for (int i = prefix.length(); i < p.length(); i++) {
            char c = p.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '+' || c == '/' || c == '=';
            if (!ok) {
                throw bad("Invalid photo data");
            }
        }
    }

    private static String grade(double p) {
        if (p >= 90) return "A+";
        if (p >= 80) return "A";
        if (p >= 70) return "B";
        if (p >= 60) return "C";
        if (p >= 50) return "D";
        return "F";
    }

    /** CSV escaping, plus protection against spreadsheet formula injection. */
    private static String csv(String value) {
        String v = value == null ? "" : value;
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
