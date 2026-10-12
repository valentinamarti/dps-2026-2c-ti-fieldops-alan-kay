package ar.edu.itba.dps.fieldops.business.exceptions;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(ExpeditionStatus from, ExpeditionStatus to) {
        super("An expedition cannot go from %s to %s".formatted(from, to));
    }
}
