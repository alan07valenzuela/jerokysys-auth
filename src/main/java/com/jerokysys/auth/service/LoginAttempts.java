package com.jerokysys.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttempts {

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration lockDuration;

    public LoginAttempts(@Value("${login.max-attempts}") int maxAttempts,
                         @Value("${login.lock-minutes}") long lockMinutes) {
        this.maxAttempts = maxAttempts;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
    }

    /**
     * Segundos restantes de bloqueo; 0 si el usuario puede intentar.
     */
    public long retryAfterSeconds(String key) {
        Attempt attempt = attempts.get(key);
        if (attempt == null || attempt.failures() < maxAttempts) {
            return 0;
        }
        long remaining = attempt.lockedUntil().getEpochSecond() - Instant.now().getEpochSecond();
        if (remaining <= 0) {
            attempts.remove(key);
            return 0;
        }
        return remaining;
    }

    public void recordFailure(String key) {
        attempts.compute(key, (k, current) -> {
            Instant now = Instant.now();
            int failures = 1;
            Instant lockedUntil = now;
            if (current != null && current.lockedUntil().isAfter(now)) {
                failures = current.failures() + 1;
                lockedUntil = current.lockedUntil();
            }
            if (failures >= maxAttempts) {
                lockedUntil = now.plus(lockDuration);
            }
            return new Attempt(failures, lockedUntil);
        });
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    private record Attempt(int failures, Instant lockedUntil) {
    }
}