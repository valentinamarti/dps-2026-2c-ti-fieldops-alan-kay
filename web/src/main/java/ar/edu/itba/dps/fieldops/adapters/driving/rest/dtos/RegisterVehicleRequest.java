package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.Set;

/**
 * The body of {@code POST /api/vehicles}. The license plate is what separates a vehicle from an instrument,
 * so it is a field of this request and not an optional field of a shared equipment body (D52).
 */
public record RegisterVehicleRequest(String id, String name, String category, String licensePlate,
                                     Set<String> certifications) {
}
