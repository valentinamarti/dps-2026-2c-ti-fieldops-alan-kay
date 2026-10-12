package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ActivityTypeResponse;
import ar.edu.itba.dps.fieldops.business.interfaces.activities.ActivityType;

/**
 * Translates a type of activity into the DTO the API publishes (D49). It only goes one way: a type is not
 * registered over HTTP, the domain is the one that says which ones there are (D44).
 */
public final class ActivityTypeMapper {

    private ActivityTypeMapper() {
    }

    public static ActivityTypeResponse toResponse(ActivityType type) {
        return new ActivityTypeResponse(type.name().value(), type.parameterNames());
    }
}
