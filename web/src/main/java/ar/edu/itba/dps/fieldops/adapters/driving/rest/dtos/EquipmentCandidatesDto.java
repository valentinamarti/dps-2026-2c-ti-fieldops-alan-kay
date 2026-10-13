package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

public record EquipmentCandidatesDto(EquipmentRequirementDto requirement, List<VehicleResponse> vehicles,
                                     List<InstrumentResponse> instruments) {
}
