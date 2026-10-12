package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

/**
 * One equipment requirement of a scheduled activity and the equipment of the catalog that covers it: of its
 * category and free during its scheduled period. Vehicles and instruments travel in their own list, like in
 * {@code GET /api/equipment} (D52), because a candidate that qualifies can be either kind.
 */
public record EquipmentCandidatesDto(EquipmentRequirementDto requirement, List<VehicleResponse> vehicles,
                                     List<InstrumentResponse> instruments) {
}
