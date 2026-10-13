package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ActivityTypeResponse;
import ar.edu.itba.dps.fieldops.business.interfaces.activities.ActivityType;

public final class ActivityTypeMapper {
    private ActivityTypeMapper() {
    }

    public static ActivityTypeResponse toResponse(ActivityType type) {
        return new ActivityTypeResponse(type.name().value(), type.parameterNames());
    }
}
