package ar.edu.itba.dps.fieldops.business.replanning;

import ar.edu.itba.dps.fieldops.business.interfaces.replanning.ReplanReason;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ReplannedActivity;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ResourceAssignment;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A resource of the affected activity is withdrawn and another one takes its place, in the same period.
 */
public record ResourceReplaced(ResourceId withdrawn, ResourceAssignment replacement) implements ReplanReason {

    public ResourceReplaced {
        Objects.requireNonNull(withdrawn, "withdrawn is required");
        Objects.requireNonNull(replacement, "replacement is required");
    }

    @Override
    public Optional<ReplannedActivity> replan(ItineraryItem affected) {
        return Optional.of(new ReplannedActivity(affected.getScheduledPeriod(),
                candidate -> !candidate.getId().equals(withdrawn), List.of(replacement)));
    }
}
