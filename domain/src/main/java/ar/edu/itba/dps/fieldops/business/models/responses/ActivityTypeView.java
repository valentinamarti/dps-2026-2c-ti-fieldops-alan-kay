package ar.edu.itba.dps.fieldops.business.models.responses;

import ar.edu.itba.dps.fieldops.business.interfaces.activities.ActivityType;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityTypeName;

import java.util.List;

public record ActivityTypeView(ActivityTypeName name, List<String> parameterNames) {

    public static ActivityTypeView of(ActivityType type) {
        return new ActivityTypeView(type.name(), type.parameterNames());
    }
}
