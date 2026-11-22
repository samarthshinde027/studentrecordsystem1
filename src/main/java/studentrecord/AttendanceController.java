package studentrecord;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import studentrecord.Dtos.AttendanceEntry;
import studentrecord.Dtos.AttendanceRequest;
import studentrecord.Dtos.AttendanceRow;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceRepository repo;

    public AttendanceController(AttendanceRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<AttendanceRow> forDate(@RequestParam String date) {
        return repo.forDate(parse(date));
    }

    @PostMapping
    public Map<String, Integer> save(@RequestBody AttendanceRequest req) {
        String date = parse(req.date());
        if (req.records() == null || req.records().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nothing to save");
        }
        List<AttendanceEntry> clean = new ArrayList<>();
        for (AttendanceEntry e : req.records()) {
            String status = e.status() == null ? "" : e.status().trim().toUpperCase();
            if (e.prn() == null || e.prn().isBlank()
                    || !(status.equals("PRESENT") || status.equals("ABSENT"))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid attendance entry");
            }
            clean.add(new AttendanceEntry(e.prn().trim(), status));
        }
        repo.saveAll(date, clean);
        return Map.of("saved", clean.size());
    }

    private static String parse(String date) {
        try {
            return LocalDate.parse(date).toString();
        } catch (DateTimeParseException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date (use YYYY-MM-DD)");
        }
    }
}
