package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.UnavailabilityDto;
import ar.edu.itba.dps.fieldops.business.models.resources.Unavailability;

public final class UnavailabilityMapper {
    private UnavailabilityMapper() {
    }

    public static Unavailability toUnavailability(UnavailabilityDto request) {
        return new Unavailability(Mappings.toPeriod(request.period()), request.reason());
    }
}
