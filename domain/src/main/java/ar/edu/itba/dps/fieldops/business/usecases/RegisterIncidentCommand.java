package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentReport;

import java.util.Objects;

public record RegisterIncidentCommand(ExpeditionId expeditionId, ActivityId activityId, IncidentReport report) {

    public RegisterIncidentCommand {
        Objects.requireNonNull(expeditionId, "expeditionId is required");
        Objects.requireNonNull(activityId, "activityId is required");
        Objects.requireNonNull(report, "report is required");
    }
}
