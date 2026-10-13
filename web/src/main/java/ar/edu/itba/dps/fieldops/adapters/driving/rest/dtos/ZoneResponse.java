package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

public record ZoneResponse(String id, String name, List<String> requiredPermits) {
}
