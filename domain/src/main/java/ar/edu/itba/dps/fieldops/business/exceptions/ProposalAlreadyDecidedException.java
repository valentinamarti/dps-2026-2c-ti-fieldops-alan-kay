package ar.edu.itba.dps.fieldops.business.exceptions;

import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalId;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalStatus;

public class ProposalAlreadyDecidedException extends RuntimeException {

    public ProposalAlreadyDecidedException(ReplanProposalId proposal, ReplanProposalStatus status) {
        super("The replan proposal %s is no longer pending: it is %s".formatted(proposal, status));
    }
}
