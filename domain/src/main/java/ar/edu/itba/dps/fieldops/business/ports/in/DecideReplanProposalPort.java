package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.commands.ReplanDecisionCommand;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalId;
import ar.edu.itba.dps.fieldops.business.models.responses.ExpeditionView;

public interface DecideReplanProposalPort {
    ExpeditionView accept(ReplanProposalId proposalId, int optionNumber, ReplanDecisionCommand command);

    void reject(ReplanProposalId proposalId, ReplanDecisionCommand command);
}
