package ar.edu.itba.dps.fieldops.business.exceptions;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;

public class NotAResponsibleException extends RuntimeException {

    public NotAResponsibleException(ResourceId person, ExpeditionId expedition) {
        super("%s is not a responsible of the expedition %s".formatted(person, expedition));
    }
}
