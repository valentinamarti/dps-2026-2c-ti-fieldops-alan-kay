package ar.edu.itba.dps.fieldops.adapters.driven;

import java.time.Duration;
import java.util.Objects;

public record RetryPolicy(int maxAttempts, Duration initialBackoff, Duration maxBackoff) {

    public RetryPolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("a delivery needs at least one attempt, but got " + maxAttempts);
        }
        Objects.requireNonNull(initialBackoff, "initial backoff is required");
        Objects.requireNonNull(maxBackoff, "max backoff is required");
        if (!initialBackoff.isPositive()) {
            throw new IllegalArgumentException("the initial backoff has to be positive, but got " + initialBackoff);
        }
        if (maxBackoff.compareTo(initialBackoff) < 0) {
            throw new IllegalArgumentException("the max backoff %s is shorter than the initial backoff %s"
                    .formatted(maxBackoff, initialBackoff));
        }
    }

    public boolean exhaustedAfter(int attempts) {
        return attempts >= maxAttempts;
    }

    public Duration backoffAfter(int attempts) {
        if (attempts < 1) {
            throw new IllegalArgumentException("there is no backoff before the first attempt, but got " + attempts);
        }
        var backoff = initialBackoff;
        for (var attempt = 1; attempt < attempts && backoff.compareTo(maxBackoff) < 0; attempt++) {
            backoff = backoff.multipliedBy(2);
        }
        return backoff.compareTo(maxBackoff) < 0 ? backoff : maxBackoff;
    }
}
