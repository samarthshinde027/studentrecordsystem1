package studentrecord;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.Validator;
import studentrecord.Dtos.ImportResult;
import studentrecord.Dtos.PageResult;

/** REST API: /api/students */
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentRepository repo;
    private final Validator validator;

    public StudentController(StudentRepository repo, Validator validator) {
        this.repo = repo;
        this.validator = validator;
    }

    @GetMapping
    public PageResult<Student> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String dir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        int safeSize = Math.max(1, Math.min(size, 100));
        int safePage = Math.max(0, page);
        return repo.search(q, course, year, sort, dir, safePage, safeSize);
    }

    @GetMapping("/courses")
    public List<String> courses() {
        return repo.courses();
    }

    @GetMapping("/{prn}")
    public Student get(@PathVariable String prn) {
        return repo.findByPrn(prn).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Student create(@Valid @RequestBody Student body) {
        Student s = body.trimmed();
        if (repo.findByPrn(s.prn()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate PRN");
        }
        try {
            repo.insert(s);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate PRN");
        }
        return s;
    }

    @PutMapping("/{prn}")
    public Student update(@PathVariable String prn, @Valid @RequestBody Student body) {
        Student t = body.trimmed();
        Student s = new Student(prn, t.name(), t.course(), t.year(), t.phone());
        if (repo.update(s) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
        }
        return s;
    }

    @DeleteMapping("/{prn}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String prn) {
        if (repo.delete(prn) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
        }
    }

    /** Bulk import (the frontend parses the CSV and sends rows as JSON). */
    @PostMapping("/import")
    public ImportResult importStudents(@RequestBody List<Student> rows) {
        if (rows.size() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Import at most 1000 rows at a time");
        }
        int added = 0;
        int skipped = 0;
        int n = 0;
        List<String> errors = new ArrayList<>();
        for (Student raw : rows) {
            n++;
            if (raw == null) {
                skipped++;
                continue;
            }
            Student s = raw.trimmed();
            String problem = validate(s);
            if (problem == null && repo.findByPrn(s.prn()).isPresent()) {
                problem = "duplicate PRN " + s.prn();
            }
            if (problem != null) {
                skipped++;
                if (errors.size() < 20) {
                    errors.add("Row " + n + ": " + problem);
                }
                continue;
            }
            try {
                repo.insert(s);
                added++;
            } catch (DataIntegrityViolationException e) {
                skipped++;
            }
        }
        return new ImportResult(added, skipped, errors);
    }

    private String validate(Student s) {
        String message = validator.validate(s).stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .sorted()
                .collect(Collectors.joining(", "));
        return message.isEmpty() ? null : message;
    }
}
