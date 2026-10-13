package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.Set;

public record RegisterStaffRequest(String id, String name, Set<String> certifications) {
}
