package com.bookleaf.support.service;

import com.bookleaf.support.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginAttemptServiceTest {

    @Test
    void blocksRepeatedFailuresAndClearsTheBlockAfterSuccessfulLogin() {
        LoginAttemptService service = new LoginAttemptService(() -> 0L);

        for (int i = 0; i < LoginAttemptService.MAX_FAILURES; i++) {
            service.recordFailure("127.0.0.1", "author@example.com");
        }

        assertThrows(TooManyRequestsException.class,
                () -> service.assertAllowed("127.0.0.1", "author@example.com"));

        service.recordSuccess("127.0.0.1", "author@example.com");

        assertDoesNotThrow(() -> service.assertAllowed("127.0.0.1", "author@example.com"));
    }

    @Test
    void expiresFailuresAfterTheConfiguredWindow() {
        AtomicLong clock = new AtomicLong();
        LoginAttemptService service = new LoginAttemptService(clock::get);

        for (int i = 0; i < LoginAttemptService.MAX_FAILURES; i++) {
            service.recordFailure("127.0.0.1", "author@example.com");
        }
        clock.set(Duration.ofMinutes(15).toMillis());

        assertDoesNotThrow(() -> service.assertAllowed("127.0.0.1", "author@example.com"));
    }

    @Test
    void evictsOldKeysWhenTheTrackingCapacityIsReached() {
        AtomicLong clock = new AtomicLong();
        LoginAttemptService service = new LoginAttemptService(clock::get);
        String oldestEmail = "oldest@example.com";
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES; i++) {
            service.recordFailure("127.0.0.1", oldestEmail);
        }
        assertThrows(TooManyRequestsException.class, () -> service.assertAllowed("127.0.0.1", oldestEmail));

        for (int i = 0; i < LoginAttemptService.MAX_TRACKED_KEYS; i++) {
            clock.incrementAndGet();
            service.recordFailure("127.0.0.1", "author-" + i + "@example.com");
        }
        clock.incrementAndGet();
        service.recordFailure("127.0.0.1", "last@example.com");

        assertDoesNotThrow(() -> service.assertAllowed("127.0.0.1", oldestEmail));
    }
}
