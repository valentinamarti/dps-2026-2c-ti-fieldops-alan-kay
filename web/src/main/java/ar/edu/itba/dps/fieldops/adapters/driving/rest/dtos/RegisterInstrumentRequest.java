package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.Set;

/**
 * The body of {@code POST /api/instruments}: the equipment that is not a vehicle, so it has no license plate.
 */
public record RegisterInstrumentRequest(String id, String name, String category, Set<String> certifications) {
}
