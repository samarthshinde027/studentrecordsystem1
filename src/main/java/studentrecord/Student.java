package studentrecord;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Model class: one student record (with validation rules). */
public record Student(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9]{3,20}$", message = "must be 3-20 letters or digits") String prn,
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Size(max = 60) String course,
        @NotNull @Min(1) @Max(6) Integer year,
        @Pattern(regexp = "^$|^[0-9]{10}$", message = "must be exactly 10 digits") String phone) {

    /** Returns a copy with all text fields trimmed (phone never null). */
    public Student trimmed() {
        return new Student(
                prn == null ? null : prn.trim(),
                name == null ? null : name.trim(),
                course == null ? null : course.trim(),
                year,
                phone == null ? "" : phone.trim());
    }
}
