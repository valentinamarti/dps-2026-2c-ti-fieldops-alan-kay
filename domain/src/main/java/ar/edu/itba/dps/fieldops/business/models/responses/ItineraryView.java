package ar.edu.itba.dps.fieldops.business.models.responses;

import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;

import java.util.List;

public record ItineraryView(ExpeditionId expeditionId, List<ItineraryNodeView> nodes) {

    public static ItineraryView of(Expedition expedition) {
        return new ItineraryView(expedition.getId(),
                expedition.getItinerary().getNodes().stream().map(ItineraryNodeView::of).toList());
    }
}
