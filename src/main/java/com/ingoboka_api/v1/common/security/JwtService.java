package com.ingoboka_api.v1.common.security;

import com.ingoboka_api.v1.common.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(resolveSecretBytes(jwtProperties.getSecret()));
    }

    public String generateAccessToken(IngobokaUserDetails userDetails) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(jwtProperties.getAccessTokenExpirationMinutes() * 60);

        return Jwts.builder()
                .subject(userDetails.getEmail())
                .claim("uid", userDetails.getUserId().toString())
                .claim("org", userDetails.getOrganizationId() != null
                        ? userDetails.getOrganizationId().toString()
                        : null)
                .claim("roles", userDetails.getRoleCodes().stream().toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, IngobokaUserDetails userDetails) {
        Claims claims = parseClaims(token);
        String email = claims.getSubject();
        Date expiration = claims.getExpiration();
        return email.equalsIgnoreCase(userDetails.getEmail()) && expiration.after(new Date());
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return parseClaims(token).get("roles", List.class);
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).get("uid", String.class));
    }

    private static byte[] resolveSecretBytes(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is not configured");
        }
        try {
            return Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ex) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }
}
