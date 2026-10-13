package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;

import java.util.List;

public record EstimateResponse(long totalDurationMinutes, RiskLevel overallRisk,
                               List<SupplyRequirementDto> estimatedConsumption) {
}
