package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

/**
 * A person of the catalog as the API shows it. The certifications come sorted and normalized, because that is
 * how the domain keeps them (D34).
 */
public record StaffResponse(String id, String name, List<String> certifications) {
}
