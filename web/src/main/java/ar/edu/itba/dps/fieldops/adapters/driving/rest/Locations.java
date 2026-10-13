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

    /**
     * Where an expedition can be read. Besides {@code ExpeditionController}, accepting a replan proposal
     * creates one (the alternative, as a draft) and has to name it too (D58).
     */
    static URI expedition(String expeditionId) {
        return URI.create("/api/expeditions/" + expeditionId);
    }

    /**
     * Where a replan proposal can be read. Registering an incident creates one without being the owner of the
     * resource, so the address is here and not inlined in two controllers (D58).
     */
    static URI replanProposal(String proposalId) {
        return URI.create("/api/replan-proposals/" + proposalId);
    }
}
