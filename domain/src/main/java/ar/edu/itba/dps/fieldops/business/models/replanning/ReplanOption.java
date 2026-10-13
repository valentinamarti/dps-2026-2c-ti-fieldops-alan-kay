package ar.edu.itba.dps.fieldops.business.models.replanning;

import ar.edu.itba.dps.fieldops.business.interfaces.replanning.ReplanReason;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;

import java.util.Objects;

public record ReplanOption(ReplanReason reason, String summary) {
    public ReplanOption {
        Objects.requireNonNull(reason, "reason is required");
        summary = DomainArguments.requireText(summary, "summary");
    }
}
