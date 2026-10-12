package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import java.net.URI;

/**
 * The addresses the controllers promise in a {@code Location}, in one place so the same resource is not named
 * twice (D49).
 */
final class Locations {

    private Locations() {
    }

    /**
     * Where what was scheduled can be read. A node of the itinerary has no address of its own, because a block
     * has no id (D39) and an activity is only reachable through the itinerary it is in.
     */
    static URI itineraryOf(String expeditionId) {
        return URI.create("/api/expeditions/" + expeditionId + "/itinerary");
    }
}
