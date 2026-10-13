package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

public record VehicleResponse(String id, String name, String category, String licensePlate,
                              List<String> certifications) {
}
