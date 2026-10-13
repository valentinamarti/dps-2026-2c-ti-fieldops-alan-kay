package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.Set;

public record RegisterVehicleRequest(String id, String name, String category, String licensePlate,
                                     Set<String> certifications) {
}
