package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;

import java.time.LocalDateTime;
import java.util.Objects;

public record Incident(String description, LocalDateTime occurredAt) {

    public Incident {
        description = DomainArguments.requireText(description, "description");
        Objects.requireNonNull(occurredAt, "occurredAt is required");
    }
}
