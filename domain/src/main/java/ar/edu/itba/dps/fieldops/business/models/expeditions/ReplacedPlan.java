package ar.edu.itba.dps.fieldops.business.models.expeditions;

import java.util.Objects;

public record ReplacedPlan(ExpeditionId expedition, int executionRevision) {

    public ReplacedPlan {
        Objects.requireNonNull(expedition, "expedition is required");
    }

    boolean isCurrentFor(ExpeditionId expedition, int executionRevision) {
        return this.expedition.equals(expedition) && this.executionRevision == executionRevision;
    }
}
