package com.company.incidentportal.security;

import com.company.incidentportal.entity.Roles;
import com.company.incidentportal.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-123";

    private User user() {
        User u = new User();
        ReflectionTestUtils.setField(u, "id", 7L);
        u.setUsername("alice");
        u.setRole(Roles.ADMIN);
        return u;
    }

    @Test
    void shouldGenerateAndParseToken() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000);

        AuthenticatedUser parsed = jwtUtil.parse(jwtUtil.generateToken(user()));

        assertNotNull(parsed);
        assertEquals(7L, parsed.id());
        assertEquals("alice", parsed.username());
        assertTrue(parsed.isAdmin());
    }

    @Test
    void shouldRejectTamperedAndGarbageTokens() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000);
        String token = jwtUtil.generateToken(user());

        assertNull(jwtUtil.parse(token + "x"));
        assertNull(jwtUtil.parse("not-a-jwt"));
        assertNull(new JwtUtil("another-secret-another-secret-12345", 60_000).parse(token));
    }

    @Test
    void shouldRejectExpiredToken() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, -1_000);

        assertNull(jwtUtil.parse(jwtUtil.generateToken(user())));
    }

    @Test
    void shouldRefuseWeakOrMissingSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil("short", 1000));
        assertThrows(IllegalStateException.class, () -> new JwtUtil("", 1000));
    }
}
