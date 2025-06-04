package io.preboot.auth.core.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.preboot.auth.api.exception.InvalidActivationTokenException;
import io.preboot.auth.api.exception.InvalidPasswordResetTokenException;
import io.preboot.auth.core.spring.AuthSecurityProperties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final AuthSecurityProperties securityProperties;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(securityProperties.getJwtSecret().getBytes());
    }

    public String generateToken(UUID sessionId) {
        return Jwts.builder()
                .subject(sessionId.toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public UUID extractSessionId(String token) {
        return UUID.fromString(extractClaim(token, Claims::getSubject));
    }

    public String generatePasswordResetToken(UUID userId, int resetTokenVersion) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("resetTokenVersion", resetTokenVersion)
                .issuedAt(new Date())
                .expiration(Date.from(
                        Instant.now().plus(securityProperties.getPasswordResetTokenTimeoutInDays(), ChronoUnit.DAYS)))
                .signWith(getSigningKey())
                .compact();
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Claims validatePasswordResetToken(final String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            throw new InvalidPasswordResetTokenException("Invalid or expired token");
        }
    }

    public String generateActivationToken(UUID userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date())
                .expiration(Date.from(
                        Instant.now().plus(securityProperties.getActivationTokenTimeoutInDays(), ChronoUnit.DAYS)))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims validateActivationToken(final String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            throw new InvalidActivationTokenException("Invalid or expired activation token");
        }
    }
}
