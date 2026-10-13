package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.EstimateResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.SupplyRequirementDto;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.estimation.ExpeditionEstimate;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Turns the estimate of an expedition into its answer (D49). The consumption travels as a list sorted by
 * category and not as an object keyed by it, so the same estimate always reads the same way even though the
 * domain keeps it in a map; each entry is a category and a quantity, the same pair a requirement of supplies
 * is (D53).
 */
public final class EstimateMapper {

    private EstimateMapper() {
    }

    public static EstimateResponse toResponse(ExpeditionEstimate estimate) {
        return new EstimateResponse(
                estimate.totalDuration().toMinutes(),
                estimate.overallRisk(),
                toConsumptionDtos(estimate.estimatedConsumption()));
    }

    /**
     * The same category-and-quantity list, sorted by category, for any {@code Map<ResourceCategory, Quantity>}:
     * what is estimated to be consumed and, since D57, what the report shows was actually consumed. Package-
     * private so {@link ReportMapper} reuses it instead of sorting and translating the map a second time.
     */
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
