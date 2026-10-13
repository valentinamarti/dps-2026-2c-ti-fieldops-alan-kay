package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

public record AssignmentCandidatesResponse(List<StaffCandidatesDto> staff, List<EquipmentCandidatesDto> equipment,
                                           List<SupplyCandidatesDto> supplies) {
}
