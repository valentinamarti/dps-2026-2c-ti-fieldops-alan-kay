package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposal;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalId;
import ar.edu.itba.dps.fieldops.business.ports.in.QueryReplanProposalsPort;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ReplanProposalRepository;

import java.util.List;
import java.util.Objects;

public class QueryReplanProposals implements QueryReplanProposalsPort {
    private final ReplanProposalRepository proposals;
    private final ExpeditionRepository expeditions;

    public QueryReplanProposals(ReplanProposalRepository proposals, ExpeditionRepository expeditions) {
        this.proposals = Objects.requireNonNull(proposals, "proposals are required");
        this.expeditions = Objects.requireNonNull(expeditions, "expeditions are required");
    }

    @Override
    public ReplanProposal get(ReplanProposalId id) {
        return Lookups.proposal(proposals, id);
    }

    @Override
    public List<ReplanProposal> ofExpedition(ExpeditionId expeditionId) {
        return proposals.findByExpedition(Lookups.expedition(expeditions, expeditionId).getId());
    }
}
