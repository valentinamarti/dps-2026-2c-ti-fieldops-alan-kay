package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

/**
 * What the catalog can offer for each requirement of a scheduled activity, grouped the same way the activity
 * groups its own requirements (D53): one entry per requirement, with the candidates that qualify for it.
 */
public record AssignmentCandidatesResponse(List<StaffCandidatesDto> staff, List<EquipmentCandidatesDto> equipment,
                                           List<SupplyCandidatesDto> supplies) {
}
