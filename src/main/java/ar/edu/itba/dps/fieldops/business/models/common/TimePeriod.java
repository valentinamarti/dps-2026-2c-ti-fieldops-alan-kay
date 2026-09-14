package ar.edu.itba.dps.fieldops.business.models.common;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public record TimePeriod(LocalDateTime start, LocalDateTime end) {

    public TimePeriod {
        Objects.requireNonNull(start, "start is required");
        Objects.requireNonNull(end, "end is required");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("start must be before end");
        }
    }

    public boolean overlapsWith(TimePeriod other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    public boolean includes(LocalDateTime instant) {
        return !instant.isBefore(start) && instant.isBefore(end);
    }

    public Duration duration() {
        return Duration.between(start, end);
    }
}
