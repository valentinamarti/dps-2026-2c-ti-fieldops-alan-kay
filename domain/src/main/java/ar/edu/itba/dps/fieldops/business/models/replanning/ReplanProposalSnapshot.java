package ar.edu.itba.dps.fieldops.business.models.replanning;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ReplanProposalSnapshot(ReplanProposalId id, ExpeditionId originalExpedition, Incident incident,
                                     List<ReplanOption> options, ReplanProposalStatus status,
                                     Optional<ReplanDecision> decision, Optional<Integer> chosenOption,
                                     Optional<ExpeditionId> alternative, Optional<ReplanProposalId> supersededBy) {

    public ReplanProposalSnapshot {
        options = List.copyOf(options);
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(decision, "decision is required");
        Objects.requireNonNull(chosenOption, "chosenOption is required");
        Objects.requireNonNull(alternative, "alternative is required");
        Objects.requireNonNull(supersededBy, "supersededBy is required");
    }
}
