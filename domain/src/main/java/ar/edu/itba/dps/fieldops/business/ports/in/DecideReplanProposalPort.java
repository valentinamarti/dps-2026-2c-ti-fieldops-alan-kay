package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.commands.ReplanDecisionCommand;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalId;

public interface DecideReplanProposalPort {
    Expedition accept(ReplanProposalId proposalId, int optionNumber, ReplanDecisionCommand command);

    void reject(ReplanProposalId proposalId, ReplanDecisionCommand command);
}
