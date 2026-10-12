package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;

import java.util.List;

/**
 * What an expedition is estimated to take: the duration of its itinerary, the highest risk of its activities
 * and what it is going to consume, by category (D4, D39).
 *
 * @param totalDurationMinutes the duration in minutes, so the unit is in the name of the field, adding up the
 *                             root nodes: a sequential block counts the sum of its parts and a parallel one
 *                             the maximum
 * @param estimatedConsumption a category and a quantity each, sorted by category
 */
public record EstimateResponse(long totalDurationMinutes, RiskLevel overallRisk,
                               List<SupplyRequirementDto> estimatedConsumption) {
}
