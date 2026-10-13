package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.Set;

public record RegisterZoneRequest(String id, String name, Set<String> requiredPermits) {
}
