package ar.edu.itba.dps.fieldops.business.fixtures;

import ar.edu.itba.dps.fieldops.business.exceptions.DuplicateReplanProposalException;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentId;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposal;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalId;
import ar.edu.itba.dps.fieldops.business.providers.ReplanProposalRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Proposal repository for tests whose ids are predictable: "rp-1", "rp-2", ... in the order they are requested.
 */
public class SequentialReplanProposalRepository implements ReplanProposalRepository {

    private final Map<ReplanProposalId, ReplanProposal> proposals = new LinkedHashMap<>();
    private int issued = 0;

    @Override
    public ReplanProposalId nextId() {
        issued++;
        return new ReplanProposalId("rp-" + issued);
    }

    @Override
    public void add(ReplanProposal proposal) {
        if (proposals.putIfAbsent(proposal.getId(), proposal) != null) {
            throw new DuplicateReplanProposalException(proposal.getId());
        }
    }

    @Override
    public Optional<ReplanProposal> findById(ReplanProposalId id) {
        return Optional.ofNullable(proposals.get(id));
    }

    @Override
    public Optional<ReplanProposal> findByIncident(IncidentId incident) {
        return proposals.values().stream().filter(proposal -> proposal.getIncident().id().equals(incident)).findFirst();
    }

    @Override
    public List<ReplanProposal> findByExpedition(ExpeditionId expedition) {
        return proposals.values().stream().filter(proposal -> proposal.getOriginalExpedition().equals(expedition)).toList();
    }
}
