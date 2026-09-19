package com.example.authsystem.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {

    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);

    private final SecretKey key;
    private final long jwtExpirationMs;
    private final JwtParser jwtParser;

    public JwtUtils(
            @Value("${app.jwt.secret:}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long jwtExpirationMs) {
        this.jwtExpirationMs = jwtExpirationMs;

        // Ensure key is sufficiently strong (minimum 64 bytes / 512 bits for HS512)
        if (StringUtils.hasText(secret) && secret.getBytes(StandardCharsets.UTF_8).length >= 64) {
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        } else {
            // Generate a secure, cryptographically random 512-bit key to avoid hardcoded secrets in source control
            this.key = Jwts.SIG.HS512.key().build();
            log.info("Initialized secure, cryptographically random HS512 key for JWT signing.");
        }

        // Explicitly build the parser with the HS512 verification key
        this.jwtParser = Jwts.parser()
                .verifyWith(this.key)
                .build();
    }

    public String generateToken(CustomUserDetails userDetails) {
        return Jwts.builder()
                .header()
                .add("typ", "JWT")
                .and()
                .subject(userDetails.getUsername())
                .claim("userId", userDetails.getId())
                .claim("role", userDetails.getRole().name())
                .claim("email", userDetails.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(this.key, Jwts.SIG.HS512) // Explicitly configure algorithm to HS512
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Object userIdObj = getClaims(token).get("userId");
        if (userIdObj instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public String getRoleFromToken(String token) {
        return (String) getClaims(token).get("role");
    }

    public Claims getClaims(String token) {
        return this.jwtParser
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String authToken) {
        try {
            Jws<Claims> jws = this.jwtParser.parseSignedClaims(authToken);
            // Verify algorithm matches required HS512
            String algorithm = jws.getHeader().getAlgorithm();
            if (!Jwts.SIG.HS512.getId().equalsIgnoreCase(algorithm)) {
                log.error("JWT algorithm '{}' does not match expected algorithm HS512", algorithm);
                return false;
            }
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
