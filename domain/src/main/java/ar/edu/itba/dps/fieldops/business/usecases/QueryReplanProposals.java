package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.NotFoundException;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentId;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalId;
import ar.edu.itba.dps.fieldops.business.models.responses.ReplanProposalView;
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
    public ReplanProposalView get(ReplanProposalId id) {
        return ReplanProposalView.of(Lookups.proposal(proposals, id));
    }

    @Override
    public List<ReplanProposalView> ofExpedition(ExpeditionId expeditionId) {
        return proposals.findByExpedition(Lookups.expedition(expeditions, expeditionId).getId()).stream()
                .map(ReplanProposalView::of)
                .toList();
    }

    @Override
    public ReplanProposalView ofIncident(IncidentId incident) {
        return proposals.findByIncident(incident).map(ReplanProposalView::of)
                .orElseThrow(() -> new NotFoundException("replan proposal for the incident", incident));
    }
}
