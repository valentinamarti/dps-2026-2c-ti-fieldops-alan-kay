package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

public record Incident(IncidentId id, ActivityId activity, String description, LocalDateTime occurredAt,
                       Optional<ResourceId> resource, Optional<Duration> expectedDelay) {
    public Incident {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(activity, "activity is required");
        description = DomainArguments.requireText(description, "description");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
        Objects.requireNonNull(resource, "resource is required");
        Objects.requireNonNull(expectedDelay, "expectedDelay is required");
    }

    static Incident registered(IncidentId id, ActivityId activity, IncidentReport report) {
        return new Incident(id, activity, report.description(), report.occurredAt(), report.resource(), report.expectedDelay());
    }
}
