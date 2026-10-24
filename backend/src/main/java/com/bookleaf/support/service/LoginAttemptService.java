package com.bookleaf.support.service;

import com.bookleaf.support.exception.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Brute-force protection for login: after MAX_FAILURES wrong passwords for the same (client IP + email)
 * within WINDOW, further attempts are rejected with 429 until the window passes. Keying on IP + email
 * (not email alone) stops an attacker from locking a real author out of their own account.
 * In-memory, so it is per instance; for several instances move the counters to Redis.
 */
@Service
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    static final int MAX_TRACKED_KEYS = 10_000;

    private record Entry(int failures, long firstFailureMillis) {}

    private final Map<String, Entry> attempts = new ConcurrentHashMap<>();
    private final LongSupplier clock;

    public LoginAttemptService() { this(System::currentTimeMillis); }

    LoginAttemptService(LongSupplier clock) { this.clock = clock; }

    /** Throws {@link TooManyRequestsException} if this client is currently blocked. */
    public void assertAllowed(String clientIp, String email) {
        String key = key(clientIp, email);
        Entry e = attempts.get(key);
        if (e == null) return;
        long elapsed = clock.getAsLong() - e.firstFailureMillis();
        if (elapsed >= WINDOW.toMillis()) {
            attempts.remove(key);
        } else if (e.failures() >= MAX_FAILURES) {
            long retry = Math.max(1, (WINDOW.toMillis() - elapsed) / 1000);
            throw new TooManyRequestsException("Too many failed login attempts. Please try again later.", retry);
        }
    }

    public synchronized void recordFailure(String clientIp, String email) {
        long now = clock.getAsLong();
        String attemptKey = key(clientIp, email);
        if (!attempts.containsKey(attemptKey) && attempts.size() >= MAX_TRACKED_KEYS) {
            evictExpired(now);
            while (attempts.size() >= MAX_TRACKED_KEYS) evictOldest();
        }
        attempts.merge(attemptKey, new Entry(1, now), (old, ignored) ->
                now - old.firstFailureMillis() >= WINDOW.toMillis() ? new Entry(1, now) : new Entry(old.failures() + 1, old.firstFailureMillis()));
    }

    public void recordSuccess(String clientIp, String email) {
        attempts.remove(key(clientIp, email));
    }

    private void evictExpired(long now) {
        attempts.forEach((key, entry) -> {
            if (now - entry.firstFailureMillis() >= WINDOW.toMillis()) attempts.remove(key, entry);
        });
    }

    private void evictOldest() {
        String oldestKey = null;
        long oldestFailureMillis = Long.MAX_VALUE;
        for (Map.Entry<String, Entry> entry : attempts.entrySet()) {
            if (entry.getValue().firstFailureMillis() < oldestFailureMillis) {
                oldestKey = entry.getKey();
                oldestFailureMillis = entry.getValue().firstFailureMillis();
            }
        }
        if (oldestKey != null) attempts.remove(oldestKey);
    }

    private static String key(String ip, String email) {
        return (ip == null ? "?" : ip) + "|" + (email == null ? "" : email.trim().toLowerCase());
    }
}
