package ru.artem.highload.social.web.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class JwtUtil {

    private final String secret;
    private final long expirationMs;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    public String generateToken(Long userId) {
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        long now = System.currentTimeMillis() / 1000;
        long exp = now + expirationMs / 1000;
        String payloadJson = "{\"sub\":\"" + userId + "\",\"iat\":" + now + ",\"exp\":" + exp + "}";
        String payload = base64Url(payloadJson);
        String signature = sign(header + "." + payload);
        return header + "." + payload + "." + signature;
    }

    private String base64Url(String input) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign JWT", e);
        }
    }
}
