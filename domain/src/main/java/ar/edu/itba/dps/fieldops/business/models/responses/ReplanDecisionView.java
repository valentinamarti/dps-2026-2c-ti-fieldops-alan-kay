package ar.edu.itba.dps.fieldops.business.models.responses;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanDecision;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposal;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;

import java.time.LocalDateTime;
import java.util.Optional;

public record ReplanDecisionView(ResourceId responsibleId, String justification, LocalDateTime decidedAt,
                                 Optional<Integer> chosenOptionNumber, Optional<ExpeditionId> alternative) {

    static ReplanDecisionView of(ReplanProposal proposal, ReplanDecision decision) {
        return new ReplanDecisionView(decision.responsible().getId(), decision.justification(), decision.decidedAt(),
                proposal.getChosenOptionNumber(), proposal.getAlternative());
    }
}
