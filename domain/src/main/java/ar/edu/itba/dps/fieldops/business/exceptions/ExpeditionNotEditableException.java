package ar.edu.itba.dps.fieldops.business.exceptions;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;

public class ExpeditionNotEditableException extends RuntimeException {

    public ExpeditionNotEditableException(ExpeditionStatus status) {
        super("The itinerary can only change while the expedition is a draft, but it is %s".formatted(status));
    }
}