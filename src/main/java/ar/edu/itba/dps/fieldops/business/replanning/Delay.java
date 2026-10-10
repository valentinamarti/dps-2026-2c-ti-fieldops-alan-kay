package ar.edu.itba.dps.fieldops.business.replanning;

import ar.edu.itba.dps.fieldops.business.interfaces.replanning.ReplanReason;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ReplannedActivity;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public record Delay(Duration delay) implements ReplanReason {

    public Delay {
        Objects.requireNonNull(delay, "delay is required");
        if (delay.isNegative() || delay.isZero()) {
            throw new IllegalArgumentException("a delay must be positive");
        }
    }

    @Override
    public Optional<ReplannedActivity> replan(ItineraryItem affected) {
        return Optional.of(new ReplannedActivity(affected.getScheduledPeriod().shiftedBy(delay), _ -> true));
    }
}
