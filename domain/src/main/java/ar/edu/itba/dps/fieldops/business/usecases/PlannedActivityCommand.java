package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;

import java.util.List;
import java.util.Map;

/**
 * The description of one activity to schedule, with the zone and the dependencies referenced by id and the
 * behaviour asked for by the name of a type with its parameters (D38, D53).
 *
 * <p>It is a piece of its own because it is the same description in the command of a single activity and in
 * each leaf of the tree of a block (D54): both are resolved by the same code.
 *
 * <p>The type name and the parameters are the ones {@code GET /api/activity-types} publishes, and they arrive
 * as text and integers because that is what an adapter receives: the use case is the one that turns them into
 * the {@code ActivityRules} of the activity, wrapped in the night variant when {@code atNight} is asked for
 * (D42, D44).
 *
 * @param dependencyIds the activities that have to finish before this one starts: ones already scheduled in
 *                      the same expedition, or earlier leaves of the same tree
 */
public record PlannedActivityCommand(ActivityId id, String name, TimePeriod timeWindow, TimePeriod scheduledPeriod,
                                     String zoneId, List<ActivityId> dependencyIds, String typeName,
                                     Map<String, Integer> parameters, boolean atNight) {
}
