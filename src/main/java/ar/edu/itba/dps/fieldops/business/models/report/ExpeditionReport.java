package ar.edu.itba.dps.fieldops.business.models.report;

import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.estimation.ExpeditionEstimate;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.validation.AcceptedWarning;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ExpeditionReport(String expeditionName, ExpeditionStatus status, int scheduledActivities,
                               int finishedActivities, ExpeditionEstimate estimate,
                               Map<ResourceCategory, Quantity> actualConsumption, List<Incident> incidents,
                               List<AcceptedWarning> acceptedWarnings) {

    public ExpeditionReport {
        expeditionName = DomainArguments.requireText(expeditionName, "expeditionName");
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(estimate, "estimate is required");
        actualConsumption = Map.copyOf(actualConsumption);
        incidents = List.copyOf(incidents);
        acceptedWarnings = List.copyOf(acceptedWarnings);
    }

    public boolean isComplete() {
        return scheduledActivities == finishedActivities;
    }
}
