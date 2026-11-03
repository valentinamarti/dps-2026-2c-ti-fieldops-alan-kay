package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.responses.ExpeditionView;
import ar.edu.itba.dps.fieldops.business.models.responses.ItineraryView;

import java.util.List;

public interface QueryExpeditionsPort {
    ExpeditionView get(ExpeditionId id);

    List<ExpeditionView> list();

    ItineraryView itinerary(ExpeditionId id);
}
