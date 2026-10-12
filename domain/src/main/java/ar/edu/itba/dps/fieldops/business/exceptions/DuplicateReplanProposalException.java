package ar.edu.itba.dps.fieldops.business.exceptions;

import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalId;

public class DuplicateReplanProposalException extends RuntimeException {

    public DuplicateReplanProposalException(ReplanProposalId id) {
        super("There is already a replan proposal with id %s".formatted(id));
    }
}
