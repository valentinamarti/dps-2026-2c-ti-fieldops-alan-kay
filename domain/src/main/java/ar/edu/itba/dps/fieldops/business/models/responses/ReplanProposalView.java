package ar.edu.itba.dps.fieldops.business.models.responses;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposal;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalId;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalStatus;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public record ReplanProposalView(ReplanProposalId id, ExpeditionId originalExpedition, ReplanProposalStatus status,
                                 Incident incident, List<ReplanOptionView> options,
                                 Optional<ReplanDecisionView> decision, Optional<ReplanProposalId> supersededBy) {

    public static ReplanProposalView of(ReplanProposal proposal) {
        final var options = proposal.getOptions();
        return new ReplanProposalView(
                proposal.getId(),
                proposal.getOriginalExpedition(),
                proposal.getStatus(),
                proposal.getIncident(),
                IntStream.rangeClosed(1, options.size())
                        .mapToObj(number -> new ReplanOptionView(number, options.get(number - 1).summary()))
                        .toList(),
                proposal.getDecision().map(decision -> ReplanDecisionView.of(proposal, decision)),
                proposal.getSupersededBy());
    }
}
