package ar.edu.itba.dps.fieldops.business.models.commands;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;

public record ScheduleBlockCommand(ExpeditionId expeditionId, PlannedNodeCommand block) {
}
