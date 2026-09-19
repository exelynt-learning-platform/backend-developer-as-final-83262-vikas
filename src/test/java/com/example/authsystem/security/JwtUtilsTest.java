package com.example.authsystem.security;

import com.example.authsystem.entity.Role;
import com.example.authsystem.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        // Test with empty secret (auto-generates secure HS512 key)
        jwtUtils = new JwtUtils("", 3600000);

        User user = User.builder()
                .id(42L)
                .username("testuser")
                .email("test@example.com")
                .password("encoded_pass")
                .role(Role.USER)
                .build();
        userDetails = new CustomUserDetails(user);
    }

    @Test
    @DisplayName("Should successfully generate and validate HS512 JWT token")
    void testGenerateAndValidateToken() {
        String token = jwtUtils.generateToken(userDetails);

        assertNotNull(token);
        assertTrue(jwtUtils.validateToken(token));
        assertEquals("testuser", jwtUtils.getUsernameFromToken(token));
        assertEquals(42L, jwtUtils.getUserIdFromToken(token));
        assertEquals("USER", jwtUtils.getRoleFromToken(token));
    }

    @Test
    @DisplayName("Should reject invalid or malformed tokens")
    void testValidateInvalidToken() {
        assertFalse(jwtUtils.validateToken("invalid.token.structure"));
        assertFalse(jwtUtils.validateToken(""));
        assertFalse(jwtUtils.validateToken(null));
    }

    @Test
    @DisplayName("Should support custom sufficiently long secret key")
    void testCustomSufficientSecret() {
        String strongSecret = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()_+~";
        JwtUtils customJwt = new JwtUtils(strongSecret, 3600000);

        String token = customJwt.generateToken(userDetails);
        assertTrue(customJwt.validateToken(token));
        assertEquals("testuser", customJwt.getUsernameFromToken(token));
    }

    @Test
    @DisplayName("Should detect expired token")
    void testExpiredToken() {
        JwtUtils expiredJwtUtils = new JwtUtils("", -1000);
        String token = expiredJwtUtils.generateToken(userDetails);

        assertFalse(expiredJwtUtils.validateToken(token));
    }
}
