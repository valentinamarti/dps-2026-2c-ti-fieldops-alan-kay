package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;

import java.time.LocalDateTime;
import java.util.Objects;

public record ReplanDecisionCommand(ResourceId responsibleId, String justification, LocalDateTime decidedAt) {

    public ReplanDecisionCommand {
        Objects.requireNonNull(responsibleId, "responsibleId is required");
        Objects.requireNonNull(decidedAt, "decidedAt is required");
    }
}
