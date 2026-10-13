package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

public record ItineraryResponse(String expeditionId, List<ItineraryNodeResponse> nodes) {
}
