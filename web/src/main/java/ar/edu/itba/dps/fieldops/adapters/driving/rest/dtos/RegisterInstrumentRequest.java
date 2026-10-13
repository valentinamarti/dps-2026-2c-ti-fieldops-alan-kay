package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.Set;

public record RegisterInstrumentRequest(String id, String name, String category, Set<String> certifications) {
}
