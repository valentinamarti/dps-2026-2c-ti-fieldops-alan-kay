package ar.edu.itba.dps.fieldops.business.interfaces.replanning;

import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanOption;

import java.util.List;

public interface ReplanOptionGenerator {
    List<ReplanOption> optionsFor(Expedition expedition, ItineraryItem affected, Incident incident);
}
