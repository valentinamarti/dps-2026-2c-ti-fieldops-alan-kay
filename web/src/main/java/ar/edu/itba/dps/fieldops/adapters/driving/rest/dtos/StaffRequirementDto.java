package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

/**
 * How many people an activity needs and what it asks of them. The certifications come normalized, because
 * that is how the domain keeps them (D34).
 */
public record StaffRequirementDto(int headcount, List<String> certifications) {
}
