package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

/**
 * A zone as the API shows it, with the permits it requires sorted so the same zone always reads the same.
 */
public record ZoneResponse(String id, String name, List<String> requiredPermits) {
}
