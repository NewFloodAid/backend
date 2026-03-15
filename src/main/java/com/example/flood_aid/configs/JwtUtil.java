package com.example.flood_aid.configs;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private final Key secretKey;
    private final long expirationTime;

    public JwtUtil(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration-ms:86400000}") long expirationTime) {
        this.secretKey = buildSigningKey(jwtSecret);
        this.expirationTime = expirationTime;
    }

    public String generateToken(String username, String appType) {
        return generateToken(username, appType, null);
    }

    public String generateToken(String username, String appType, UUID userId) {
        JwtBuilder builder = Jwts.builder()
                .setSubject(username)
                .claim("appType", appType)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime));

        if (userId != null) {
            builder.claim("userId", userId.toString());
        }

        return builder.signWith(secretKey, SignatureAlgorithm.HS256).compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractAppType(String token) {
        return extractAllClaims(token).get("appType", String.class);
    }

    public UUID extractUserId(String token) {
        String userId = extractAllClaims(token).get("userId", String.class);
        if (userId == null || userId.isBlank()) {
            return null;
        }
        return UUID.fromString(userId);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getSubject() != null
                && claims.get("appType", String.class) != null
                && !isTokenExpired(claims);
    }

    public boolean validateToken(String token, String username, String appType) {
        String extractedUsername = extractUsername(token);
        String extractedAppType = extractAppType(token);
        return extractedUsername.equals(username)
                && extractedAppType.equals(appType)
                && !isTokenExpired(extractAllClaims(token));
    }

    private boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration == null || expiration.before(new Date());
    }

    private Key buildSigningKey(String jwtSecret) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT secret is required. Configure jwt.secret / JWT_SECRET.");
        }

        byte[] keyBytes = resolveSecretBytes(jwtSecret.trim());
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes for HS256.");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] resolveSecretBytes(String jwtSecret) {
        try {
            byte[] decoded = Decoders.BASE64.decode(jwtSecret);
            if (decoded.length >= 32) {
                return decoded;
            }
        } catch (RuntimeException ignored) {
            // Not base64; fallback to raw text bytes.
        }
        return jwtSecret.getBytes(StandardCharsets.UTF_8);
    }
}
