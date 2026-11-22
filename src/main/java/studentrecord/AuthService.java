package studentrecord;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Single admin account + stateless HMAC-signed tokens (12 hour expiry). */
@Service
public class AuthService {

    private static final long TTL_SECONDS = 12 * 3600L;

    private final String adminUser;
    private final String adminPass;
    private final byte[] secret;

    public AuthService(@Value("${app.admin.user}") String adminUser,
                       @Value("${app.admin.pass}") String adminPass,
                       @Value("${app.auth.secret:}") String secretText) {
        this.adminUser = adminUser;
        this.adminPass = adminPass;
        byte[] key;
        if (secretText == null || secretText.isBlank()) {
            key = new byte[32];
            new SecureRandom().nextBytes(key);
        } else {
            key = secretText.getBytes(StandardCharsets.UTF_8);
        }
        this.secret = key;
    }

    /** Returns a token if the credentials are correct. */
    public Optional<String> login(String user, String pass) {
        boolean ok = same(user, adminUser) & same(pass, adminPass);
        return ok ? Optional.of(issue(adminUser)) : Optional.empty();
    }

    public boolean isValid(String token) {
        if (token == null) {
            return false;
        }
        int dot = token.indexOf('.');
        if (dot < 1) {
            return false;
        }
        String payloadPart = token.substring(0, dot);
        byte[] given;
        try {
            given = Base64.getUrlDecoder().decode(token.substring(dot + 1));
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (!MessageDigest.isEqual(given, hmac(payloadPart))) {
            return false;
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(payloadPart), StandardCharsets.UTF_8);
            long expires = Long.parseLong(payload.substring(payload.lastIndexOf('|') + 1));
            return expires > Instant.now().getEpochSecond();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private String issue(String user) {
        long expires = Instant.now().getEpochSecond() + TTL_SECONDS;
        String payloadPart = encode((user + "|" + expires).getBytes(StandardCharsets.UTF_8));
        return payloadPart + "." + encode(hmac(payloadPart));
    }

    private static boolean same(String a, String b) {
        String left = a == null ? "" : a;
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
