package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

public record IncidentReport(String description, LocalDateTime occurredAt, Optional<ResourceId> resource,
                             Optional<Duration> expectedDelay) {
    public IncidentReport {
        description = DomainArguments.requireText(description, "description");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
        Objects.requireNonNull(resource, "resource is required");
        Objects.requireNonNull(expectedDelay, "expectedDelay is required");
        expectedDelay.ifPresent(IncidentReport::requirePositive);
    }

    public static IncidentReport of(String description, LocalDateTime occurredAt) {
        return new IncidentReport(description, occurredAt, Optional.empty(), Optional.empty());
    }

    public IncidentReport affecting(ResourceId resource) {
        return new IncidentReport(description, occurredAt, Optional.of(resource), expectedDelay);
    }

    public IncidentReport delayingBy(Duration delay) {
        return new IncidentReport(description, occurredAt, resource, Optional.of(delay));
    }

    private static void requirePositive(Duration delay) {
        if (delay.isNegative() || delay.isZero()) {
            throw new IllegalArgumentException("an expected delay must be positive");
        }
    }
}
