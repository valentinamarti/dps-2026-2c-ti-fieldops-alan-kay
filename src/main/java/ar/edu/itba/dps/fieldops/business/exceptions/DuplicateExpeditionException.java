package ar.edu.itba.dps.fieldops.business.exceptions;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;

public class DuplicateExpeditionException extends RuntimeException {

    public DuplicateExpeditionException(ExpeditionId expeditionId) {
        super("An expedition with id %s is already registered".formatted(expeditionId));
    }
}
