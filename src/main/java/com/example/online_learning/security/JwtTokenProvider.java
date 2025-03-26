package com.example.online_learning.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // Secret key for signing the JWT (must be at least 64 characters for HS256)
    private String secretKey = "MySuperSecretKeyForJWTGenerationMySuperSecretKeyForJWTGeneration";

    // Token validity period in milliseconds (e.g., 1 day)
    private long validityInMilliseconds = 86400000;

    private Key key;

    @PostConstruct
    protected void init() {
        // Generate a secure key based on the secretKey
        key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    // Generate a JWT token with username and role
    public String generateToken(String username, String role) {
        Claims claims = Jwts.claims().setSubject(username);
        claims.put("role", role);
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // Validate the token; return true if valid, false otherwise
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // In production, log the exception details
            return false;
        }
    }

    // Extract all claims from the token
    public Claims extractClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    // Extract token from the Authorization header in the request
    public String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
