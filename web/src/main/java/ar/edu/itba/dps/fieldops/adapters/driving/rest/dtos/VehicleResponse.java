package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

/**
 * A vehicle of the catalog as the API shows it. The category comes normalized, like the certifications (D34).
 */
public record VehicleResponse(String id, String name, String category, String licensePlate,
                              List<String> certifications) {
}
