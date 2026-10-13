package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;

import java.util.List;

public record ExpeditionReportResponse(String expeditionName, ExpeditionStatus status, int scheduledActivities,
                                       int finishedActivities, EstimateResponse estimate,
                                       List<SupplyRequirementDto> actualConsumption, List<IncidentDto> incidents,
                                       List<AcceptedWarningDto> acceptedWarnings) {
}
