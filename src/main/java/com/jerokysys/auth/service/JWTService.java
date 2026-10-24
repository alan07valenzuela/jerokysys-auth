package com.jerokysys.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

@Service
public class JWTService {

    private final SecretKey secretKey;
    private final SignatureAlgorithm algorithm;
    private final long expirationMinutes;
    private final String subject;
    private final String issuer;
    private final String userClaim;
    private final String roleClaim;

    public JWTService(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.algorithm}") String algorithmName,
                       @Value("${jwt.expiration-minutes}") long expirationMinutes,
                       @Value("${jwt.subject}") String subject,
                       @Value("${jwt.issuer}") String issuer,
                       @Value("${jwt.claim.user}") String userClaim,
                       @Value("${jwt.claim.role}") String roleClaim) {
        this.algorithm = SignatureAlgorithm.forName(algorithmName);
        this.secretKey = buildKey(secret, this.algorithm);
        this.expirationMinutes = expirationMinutes;
        this.subject = subject;
        this.issuer = issuer;
        this.userClaim = userClaim;
        this.roleClaim = roleClaim;
    }

    @PostConstruct
    void checkConfig() {
        if (!algorithm.isHmac()) {
            throw new IllegalStateException("jwt.algorithm must be HS256, HS384 or HS512");
        }
        if (expirationMinutes <= 0) {
            throw new IllegalStateException("jwt.expiration-minutes must be greater than 0");
        }
    }

    public String generate(String user, String role) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .claim(userClaim, user)
                .claim(roleClaim, role)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)));
        if (issuer != null && !issuer.isBlank()) {
            builder.issuer(issuer);
        }
        return builder.signWith(algorithm, secretKey).compact();
    }

    /** Parsea y valida un token emitido con la misma clave. */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Nombre del claim que contiene el usuario (configurable por jwt.claim.user). */
    public String userClaim() {
        return userClaim;
    }

    /** Nombre del claim que contiene el rol (configurable por jwt.claim.role). */
    public String roleClaim() {
        return roleClaim;
    }

    private static SecretKey buildKey(String secret, SignatureAlgorithm algorithm) {
        byte[] bytes = decode(secret);
        int minBits = algorithm.getMinKeyLength();
        if (bytes.length * 8 < minBits) {
            throw new IllegalStateException(
                    "jwt.secret must decode to at least " + minBits + " bits for " + algorithm.getJcaName());
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    private static byte[] decode(String secret) {
        try {
            return Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ex) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }
}
