package ar.edu.itba.dps.fieldops.business.models.commands;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;

public record ScheduleActivityCommand(ExpeditionId expeditionId, PlannedActivityCommand activity) {
}
