package ar.edu.itba.dps.fieldops.adapters.driven;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetryPolicyTest {
    private static final RetryPolicy RETRIES = new RetryPolicy(5, Duration.ofSeconds(1), Duration.ofSeconds(30));

    @Test
    void doublesTheBackoffAfterEachFailedAttempt() {
        assertEquals(List.of(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(4), Duration.ofSeconds(8)),
                IntStream.rangeClosed(1, 4).mapToObj(RETRIES::backoffAfter).toList());
    }

    @Test
    void neverWaitsLongerThanTheMaxBackoff() {
        final var patient = new RetryPolicy(100, Duration.ofSeconds(1), Duration.ofSeconds(30));

        assertEquals(Duration.ofSeconds(16), patient.backoffAfter(5));
        assertEquals(Duration.ofSeconds(30), patient.backoffAfter(6));
        assertEquals(Duration.ofSeconds(30), patient.backoffAfter(Integer.MAX_VALUE));
    }

    @Test
    void isExhaustedOnlyWhenTheLastAttemptFailed() {
        assertFalse(RETRIES.exhaustedAfter(4));
        assertTrue(RETRIES.exhaustedAfter(5));
    }

    @Test
    void rejectsValuesThatWouldNeverRetryOrNeverWait() {
        assertThrows(IllegalArgumentException.class, () -> new RetryPolicy(0, Duration.ofSeconds(1), Duration.ofSeconds(30)));
        assertThrows(IllegalArgumentException.class, () -> new RetryPolicy(5, Duration.ZERO, Duration.ofSeconds(30)));
        assertThrows(IllegalArgumentException.class, () -> new RetryPolicy(5, Duration.ofSeconds(10), Duration.ofSeconds(5)));
        assertThrows(IllegalArgumentException.class, () -> RETRIES.backoffAfter(0));
    }
}
