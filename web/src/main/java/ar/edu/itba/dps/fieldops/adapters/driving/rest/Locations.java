package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import java.net.URI;

final class Locations {
    private Locations() {
    }

    static URI itineraryOf(String expeditionId) {
        return URI.create("/api/expeditions/" + expeditionId + "/itinerary");
    }

    static URI expedition(String expeditionId) {
        return URI.create("/api/expeditions/" + expeditionId);
    }

    static URI replanProposal(String proposalId) {
        return URI.create("/api/replan-proposals/" + proposalId);
    }
}
