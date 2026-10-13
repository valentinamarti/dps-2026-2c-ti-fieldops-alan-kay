package ar.edu.itba.dps.fieldops.business.models.replanning;

import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;

import java.time.LocalDateTime;
import java.util.Objects;

public record ReplanDecision(Person responsible, String justification, LocalDateTime decidedAt) {
    public ReplanDecision {
        Objects.requireNonNull(responsible, "responsible is required");
        justification = DomainArguments.requireText(justification, "justification");
        Objects.requireNonNull(decidedAt, "decidedAt is required");
    }
}
