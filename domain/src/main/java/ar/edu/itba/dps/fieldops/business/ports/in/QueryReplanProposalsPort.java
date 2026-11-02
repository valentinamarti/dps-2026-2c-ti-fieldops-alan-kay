package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposal;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalId;

import java.util.List;

public interface QueryReplanProposalsPort {
    ReplanProposal get(ReplanProposalId id);

    List<ReplanProposal> ofExpedition(ExpeditionId expeditionId);
}
