package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;

import java.util.List;
import java.util.Map;

public record PlannedActivityCommand(ActivityId id, String name, TimePeriod timeWindow, TimePeriod scheduledPeriod,
                                     String zoneId, List<ActivityId> dependencyIds, String typeName,
                                     Map<String, Integer> parameters, boolean atNight) {
}
