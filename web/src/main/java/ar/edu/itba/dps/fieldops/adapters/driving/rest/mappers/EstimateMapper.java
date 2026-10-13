package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.EstimateResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.SupplyRequirementDto;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.estimation.ExpeditionEstimate;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class EstimateMapper {
    private EstimateMapper() {
    }

    public static EstimateResponse toResponse(ExpeditionEstimate estimate) {
        return new EstimateResponse(
                estimate.totalDuration().toMinutes(),
                estimate.overallRisk(),
                toConsumptionDtos(estimate.estimatedConsumption()));
    }

    static List<SupplyRequirementDto> toConsumptionDtos(Map<ResourceCategory, Quantity> consumption) {
        return consumption.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().name()))
                .map(EstimateMapper::toDto)
                .toList();
    }

    private static SupplyRequirementDto toDto(Map.Entry<ResourceCategory, Quantity> consumption) {
        return new SupplyRequirementDto(consumption.getKey().name(), Mappings.toDto(consumption.getValue()));
    }
}
