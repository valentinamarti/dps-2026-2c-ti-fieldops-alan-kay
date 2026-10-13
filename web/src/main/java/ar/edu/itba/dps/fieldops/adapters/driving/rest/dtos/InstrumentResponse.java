package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

public record InstrumentResponse(String id, String name, String category, List<String> certifications) {
}
