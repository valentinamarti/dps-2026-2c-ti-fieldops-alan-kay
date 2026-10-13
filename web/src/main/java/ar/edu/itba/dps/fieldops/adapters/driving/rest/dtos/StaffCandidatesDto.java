package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

public record StaffCandidatesDto(StaffRequirementDto requirement, List<StaffResponse> candidates) {
}
