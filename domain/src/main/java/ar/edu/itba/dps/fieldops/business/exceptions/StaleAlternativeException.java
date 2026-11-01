package ar.edu.itba.dps.fieldops.business.exceptions;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;

public class StaleAlternativeException extends RuntimeException {

    public StaleAlternativeException(ExpeditionId alternative, ExpeditionId original) {
        super("The alternative %s was built on a plan of the expedition %s that kept running since then"
                .formatted(alternative, original));
    }
}
