package ar.edu.itba.dps.fieldops.business.replanning;

import ar.edu.itba.dps.fieldops.business.interfaces.replanning.ReplanReason;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ReplannedActivity;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;

import java.util.Objects;
import java.util.Optional;

public record ResourceUnavailability(ResourceId resource) implements ReplanReason {

    public ResourceUnavailability {
        Objects.requireNonNull(resource, "resource is required");
    }

    @Override
    public Optional<ReplannedActivity> replan(ItineraryItem affected) {
        return Optional.of(new ReplannedActivity(affected.getScheduledPeriod(), candidate -> !candidate.getId().equals(resource)));
    }
}
