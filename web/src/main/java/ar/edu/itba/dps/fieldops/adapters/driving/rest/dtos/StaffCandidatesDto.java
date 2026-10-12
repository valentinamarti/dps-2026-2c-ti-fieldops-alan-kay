package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

/**
 * One staff requirement of a scheduled activity and the people of the catalog that qualify for it: have the
 * certifications it asks and are free during its scheduled period (and, for a night activity, are admitted by
 * its rules).
 */
public record StaffCandidatesDto(StaffRequirementDto requirement, List<StaffResponse> candidates) {
}
