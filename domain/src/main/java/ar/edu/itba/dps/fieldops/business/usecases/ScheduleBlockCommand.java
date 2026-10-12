package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;

/**
 * The block to schedule in an expedition, as a tree whose leaves are described exactly like the command of a
 * single activity (D53, D54).
 */
public record ScheduleBlockCommand(ExpeditionId expeditionId, PlannedNodeCommand block) {
}
