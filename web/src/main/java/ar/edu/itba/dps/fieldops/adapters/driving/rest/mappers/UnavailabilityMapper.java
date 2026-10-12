package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.UnavailabilityDto;
import ar.edu.itba.dps.fieldops.business.models.resources.Unavailability;

/**
 * Translates the body of the unavailabilities sub-resource, which the three kinds of reusable resource share
 * (D52), so the three controllers that offer it do not each repeat the translation.
 */
public final class UnavailabilityMapper {

    private UnavailabilityMapper() {
    }

    public static Unavailability toUnavailability(UnavailabilityDto request) {
        return new Unavailability(Mappings.toPeriod(request.period()), request.reason());
    }
}
