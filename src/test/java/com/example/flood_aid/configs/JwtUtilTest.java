package com.example.flood_aid.configs;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {
    private static final String TEST_SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void generateLiffTokenIncludesVerifiedUserIdClaim() {
        JwtUtil jwtUtil = new JwtUtil(TEST_SECRET, 86_400_000L);
        UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        String token = jwtUtil.generateToken("line-subject", "LIFF", userId);

        assertTrue(jwtUtil.validateToken(token));
        assertEquals("line-subject", jwtUtil.extractUsername(token));
        assertEquals("LIFF", jwtUtil.extractAppType(token));
        assertEquals(userId, jwtUtil.extractUserId(token));
    }

    @Test
    void generateWebTokenDoesNotRequireUserIdClaim() {
        JwtUtil jwtUtil = new JwtUtil(TEST_SECRET, 86_400_000L);

        String token = jwtUtil.generateToken("rescuer", "WEB");

        assertTrue(jwtUtil.validateToken(token));
        assertEquals("rescuer", jwtUtil.extractUsername(token));
        assertEquals("WEB", jwtUtil.extractAppType(token));
        assertNull(jwtUtil.extractUserId(token));
    }
}
