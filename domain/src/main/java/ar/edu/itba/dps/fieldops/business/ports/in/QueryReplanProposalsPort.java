package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentId;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalId;
import ar.edu.itba.dps.fieldops.business.models.responses.ReplanProposalView;

import java.util.List;

public interface QueryReplanProposalsPort {
    ReplanProposalView get(ReplanProposalId id);

    List<ReplanProposalView> ofExpedition(ExpeditionId expeditionId);

    ReplanProposalView ofIncident(IncidentId incident);
}
