package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

/**
 * One supply requirement of a scheduled activity and the consumables of the catalog with enough stock to cover
 * it.
 */
public record SupplyCandidatesDto(SupplyRequirementDto requirement, List<SupplyResponse> candidates) {
}
