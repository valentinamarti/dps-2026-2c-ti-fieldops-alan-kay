package ar.edu.itba.dps.fieldops.business.replanning;

import ar.edu.itba.dps.fieldops.business.interfaces.replanning.ReplanReason;
import ar.edu.itba.dps.fieldops.business.interfaces.replanning.ReplanReasonVisitor;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ReplannedActivity;

import java.util.Optional;

public record Cancellation() implements ReplanReason {

    @Override
    public Optional<ReplannedActivity> replan(ItineraryItem affected) {
        return Optional.empty();
    }

    @Override
    public <T> T accept(ReplanReasonVisitor<T> visitor) {
        return visitor.cancellation();
    }
}
