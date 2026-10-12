package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.Set;

/**
 * The body of {@code POST /api/zones}. The permits are the ones the zone requires; an expedition that does not
 * hold them is reported by the validation rules, not here.
 */
public record RegisterZoneRequest(String id, String name, Set<String> requiredPermits) {
}
