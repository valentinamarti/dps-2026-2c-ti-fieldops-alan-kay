package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.Set;

/**
 * The body of {@code POST /api/staff}. The id is the one whoever registers the person chooses, unlike the one
 * of an expedition, which the system gives; the catalog rejects it if it is already taken.
 */
public record RegisterStaffRequest(String id, String name, Set<String> certifications) {
}
