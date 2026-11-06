package ar.edu.itba.dps.fieldops.business.integration;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

final class MutableClock extends Clock {
    private final AtomicReference<Instant> now;

    MutableClock(LocalDateTime start) {
        this.now = new AtomicReference<>(start.toInstant(ZoneOffset.UTC));
    }

    void setTo(LocalDateTime moment) {
        now.set(moment.toInstant(ZoneOffset.UTC));
    }

    void advance(Duration duration) {
        now.updateAndGet(instant -> instant.plus(duration));
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(instant(), zone);
    }

    @Override
    public Instant instant() {
        return now.get();
    }
}
