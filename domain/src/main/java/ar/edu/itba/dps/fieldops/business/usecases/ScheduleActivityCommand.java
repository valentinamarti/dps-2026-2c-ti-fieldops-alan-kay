package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;

import java.util.List;
import java.util.Map;

/**
 * Everything needed to schedule an activity, with the zone and the dependencies referenced by id and the
 * behaviour of the activity asked for by the name of its type (D38, D53).
 *
 * <p>The type name and the parameters are the ones {@code GET /api/activity-types} publishes, and they arrive
 * as text and integers because that is what an adapter receives: {@link ScheduleActivity} is the one that
 * turns them into the {@code ActivityRules} of the activity, wrapped in the night variant when
 * {@code atNight} is asked for (D42, D44).
 *
 * @param dependencyIds the activities this one depends on, which have to be already scheduled in the same
 *                      expedition
 */
public record ScheduleActivityCommand(ExpeditionId expeditionId, ActivityId activityId, String name,
                                      TimePeriod timeWindow, TimePeriod scheduledPeriod, String zoneId,
                                      List<ActivityId> dependencyIds, String typeName,
                                      Map<String, Integer> parameters, boolean atNight) {
}
