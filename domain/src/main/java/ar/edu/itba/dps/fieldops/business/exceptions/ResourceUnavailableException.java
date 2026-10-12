package ar.edu.itba.dps.fieldops.business.exceptions;

import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;

public class ResourceUnavailableException extends RuntimeException {

    public ResourceUnavailableException(TimePeriod period) {
        super("Resource is not available during %s - %s".formatted(period.start(), period.end()));
    }
}
