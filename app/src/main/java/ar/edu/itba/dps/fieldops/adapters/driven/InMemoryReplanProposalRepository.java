package ar.edu.itba.dps.fieldops.adapters.driven;

import ar.edu.itba.dps.fieldops.business.exceptions.DuplicateReplanProposalException;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentId;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposal;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalId;
import ar.edu.itba.dps.fieldops.business.providers.ReplanProposalRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class InMemoryReplanProposalRepository implements ReplanProposalRepository {

    private final Map<ReplanProposalId, ReplanProposal> proposals = new LinkedHashMap<>();

    @Override
    public ReplanProposalId nextId() {
        return new ReplanProposalId(UUID.randomUUID().toString());
    }

    @Override
    public void add(ReplanProposal proposal) {
        Objects.requireNonNull(proposal, "proposal is required");
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
