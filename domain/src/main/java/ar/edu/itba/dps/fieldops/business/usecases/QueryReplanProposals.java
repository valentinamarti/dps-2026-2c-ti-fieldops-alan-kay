package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposal;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalId;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ReplanProposalRepository;

import java.util.List;
import java.util.Objects;

/**
 * The audit of replanning: each proposal keeps the original expedition, the incident, the options and the
 * decision. The original plan is still the original expedition, which {@link QueryExpeditions} returns (D48).
 */
public class QueryReplanProposals {

    private final ReplanProposalRepository proposals;
    private final ExpeditionRepository expeditions;

    public QueryReplanProposals(ReplanProposalRepository proposals, ExpeditionRepository expeditions) {
        this.proposals = Objects.requireNonNull(proposals, "proposals are required");
        this.expeditions = Objects.requireNonNull(expeditions, "expeditions are required");
    }

    public ReplanProposal get(ReplanProposalId id) {
        return Lookups.proposal(proposals, id);
    }

    public List<ReplanProposal> ofExpedition(ExpeditionId expeditionId) {
        return proposals.findByExpedition(Lookups.expedition(expeditions, expeditionId).getId());
    }
}
