package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

/**
 * An instrument of the catalog as the API shows it.
 */
public record InstrumentResponse(String id, String name, String category, List<String> certifications) {
}
