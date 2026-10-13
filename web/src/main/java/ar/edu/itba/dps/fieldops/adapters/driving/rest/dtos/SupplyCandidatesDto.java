package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

public record SupplyCandidatesDto(SupplyRequirementDto requirement, List<SupplyResponse> candidates) {
}
