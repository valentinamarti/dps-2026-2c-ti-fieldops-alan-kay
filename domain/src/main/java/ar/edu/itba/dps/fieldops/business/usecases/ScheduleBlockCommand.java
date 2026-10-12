package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode;

public record ScheduleBlockCommand(ExpeditionId expeditionId, PlannedNode block) {
}
