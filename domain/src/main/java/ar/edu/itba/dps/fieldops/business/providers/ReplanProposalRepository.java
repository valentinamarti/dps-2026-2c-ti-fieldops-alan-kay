package ar.edu.itba.dps.fieldops.business.providers;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentId;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposal;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalId;

import java.util.List;
import java.util.Optional;

public interface ReplanProposalRepository {
    ReplanProposalId nextId();

    void add(ReplanProposal proposal);

    Optional<ReplanProposal> findById(ReplanProposalId id);

    Optional<ReplanProposal> findByIncident(IncidentId incident);

    List<ReplanProposal> findByExpedition(ExpeditionId expedition);
}
