package com.urbanblack.driverservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

/**
 * Validates JWTs issued by the common auth-service.
 * auth-service JWT claims: subject=email, role=DRIVER|USER|ADMIN,
 * userId=<auth_user_id>
 * No token generation here — auth-service is the single issuer.
 */
@Component
public class DriverJwtUtils {

    @Value("${jwt.secret}")
    private String secretKey;

    /** Email is stored as the JWT subject by auth-service */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** Role claim set by auth-service: DRIVER / USER / ADMIN */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public boolean isTokenValid(String token) {
        try {
            return !extractClaim(token, Claims::getExpiration).before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return resolver.apply(claims);
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
