package com.bookleaf.support.config;

import com.bookleaf.support.entity.Role;
import com.bookleaf.support.entity.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "a-unique-test-signing-key-with-more-than-32-bytes";

    @Test
    void refusesMissingOrShortSigningSecrets() {
        assertThrows(IllegalStateException.class, () -> new JwtService("", 60));
        assertThrows(IllegalStateException.class, () -> new JwtService("too-short", 60));
    }

    @Test
    void extractsSubjectOnlyFromValidSignedToken() {
        JwtService service = new JwtService(SECRET, 60);
        User user = User.builder().id(12L).email("admin@example.com").role(Role.ADMIN).build();

        String token = service.generateToken(user);
        String[] parts = token.split("\\.");
        parts[2] = (parts[2].charAt(0) == 'a' ? "b" : "a") + parts[2].substring(1);

        assertEquals("admin@example.com", service.extractEmail(token));
        assertThrows(JwtException.class, () -> service.extractEmail(String.join(".", parts)));
    }

    @Test
    void rejectsExpiredTokens() {
        JwtService service = new JwtService(SECRET, -1);
        User user = User.builder().id(12L).email("admin@example.com").role(Role.ADMIN).build();

        assertThrows(JwtException.class, () -> service.extractEmail(service.generateToken(user)));
    }
}
