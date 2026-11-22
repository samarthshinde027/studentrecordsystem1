package studentrecord;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import studentrecord.Dtos.LoginRequest;
import studentrecord.Dtos.LoginResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {
        Optional<String> token = auth.login(req.username(), req.password());
        if (token.isEmpty()) {
            try {
                Thread.sleep(500); // slows down password guessing
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        return new LoginResponse(token.get(), req.username());
    }

    /** Used by the frontend to check that a stored token is still valid. */
    @GetMapping("/me")
    public Map<String, Boolean> me() {
        return Map.of("ok", true);
    }
}
