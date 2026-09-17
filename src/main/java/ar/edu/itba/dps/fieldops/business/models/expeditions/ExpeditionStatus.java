package ar.edu.itba.dps.fieldops.business.models.expeditions;

import java.util.Map;
import java.util.Set;

public enum ExpeditionStatus {
    DRAFT,
    IN_REVIEW,
    APPROVED,
    IN_EXECUTION,
    SUSPENDED,
    FINISHED;

    private static final Map<ExpeditionStatus, Set<ExpeditionStatus>> NEXT_STATUSES = Map.of(
            DRAFT, Set.of(IN_REVIEW),
            IN_REVIEW, Set.of(DRAFT, APPROVED),
            APPROVED, Set.of(IN_EXECUTION),
            IN_EXECUTION, Set.of(SUSPENDED, FINISHED),
            SUSPENDED, Set.of(IN_EXECUTION, FINISHED),
            FINISHED, Set.of()
    );

    public boolean canTransitionTo(ExpeditionStatus next) {
        return NEXT_STATUSES.getOrDefault(this, Set.of()).contains(next);
    }
}
