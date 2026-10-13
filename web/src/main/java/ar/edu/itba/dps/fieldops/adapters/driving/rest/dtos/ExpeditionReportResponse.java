package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;

import java.util.List;

/**
 * What an expedition is worth once it ran, reusing {@link EstimateResponse} for the plan side of it (D4, D57):
 * {@code estimate} is still what the itinerary was expected to take, and {@code actualConsumption} is what
 * finished activities actually used, in the same category-and-{@link QuantityDto} shape as
 * {@code EstimateResponse.estimatedConsumption} so the two read the same way side by side.
 *
 * @param scheduledActivities how many activities the itinerary has, finished or not
 * @param finishedActivities  how many of those are finished; the two together are what
 *                            {@code ExpeditionReport.isComplete()} compares in the domain
 */
public record ExpeditionReportResponse(String expeditionName, ExpeditionStatus status, int scheduledActivities,
                                       int finishedActivities, EstimateResponse estimate,
                                       List<SupplyRequirementDto> actualConsumption, List<IncidentDto> incidents,
                                       List<AcceptedWarningDto> acceptedWarnings) {
}
