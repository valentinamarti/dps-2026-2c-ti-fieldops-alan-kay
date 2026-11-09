package ar.edu.itba.dps.fieldops;

import ar.edu.itba.dps.fieldops.adapters.driven.RetryPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties("fieldops.events")
public record EventsProperties(Duration dispatchInterval, int maxAttempts, Duration initialBackoff,
                               Duration maxBackoff, boolean dispatcherEnabled) {

    public EventsProperties {
        Objects.requireNonNull(dispatchInterval, "fieldops.events.dispatch-interval is required");
        if (!dispatchInterval.isPositive()) {
            throw new IllegalArgumentException("the dispatch interval has to be positive, but got " + dispatchInterval);
        }
    }

    RetryPolicy retryPolicy() {
        return new RetryPolicy(maxAttempts, initialBackoff, maxBackoff);
    }
}
