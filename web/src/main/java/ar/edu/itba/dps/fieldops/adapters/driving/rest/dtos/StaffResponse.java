package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

public record StaffResponse(String id, String name, List<String> certifications) {
}
