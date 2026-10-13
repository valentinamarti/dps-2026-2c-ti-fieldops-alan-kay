package ar.edu.itba.dps.fieldops.business.interfaces.itinerary;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;

import java.util.List;

public interface ItineraryNodeVisitor<T> {
    T activity(ItineraryItem item);

    T sequence(ItineraryNode block, List<T> parts);

    T parallel(ItineraryNode block, List<T> branches);
}
