package ar.edu.itba.dps.fieldops.business.exceptions;

import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;

public class ResourceUnavailableException extends RuntimeException {

    public ResourceUnavailableException(TimePeriod period) {
        super("Resource is already booked during %s - %s".formatted(period.start(), period.end()));
    }
}
