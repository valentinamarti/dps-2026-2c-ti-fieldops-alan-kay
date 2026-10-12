package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;

/**
 * Everything needed to schedule an activity: the expedition it goes into and the description of the activity,
 * the same one each leaf of a block carries (D53, D54).
 */
public record ScheduleActivityCommand(ExpeditionId expeditionId, PlannedActivityCommand activity) {
}
