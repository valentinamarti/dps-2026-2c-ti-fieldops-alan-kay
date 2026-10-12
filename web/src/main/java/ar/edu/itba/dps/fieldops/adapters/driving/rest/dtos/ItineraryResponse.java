package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

/**
 * The itinerary of an expedition: its root nodes, sorted by when they start, each one with its own tree below
 * it. An itinerary without blocks reads as a list of leaves, because there every activity is its own root
 * (D39, D54).
 */
public record ItineraryResponse(String expeditionId, List<ItineraryNodeResponse> nodes) {
}
