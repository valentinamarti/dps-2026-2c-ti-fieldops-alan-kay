package ar.edu.itba.dps.fieldops.business.interfaces.replanning;

import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanOption;

import java.util.List;

/**
 * Offers the options of one kind that make sense for an incident. Whether each one can actually be built is checked
 * afterwards, so a generator only decides what to offer (D46).
 */
public interface ReplanOptionGenerator {

    List<ReplanOption> optionsFor(Expedition expedition, ItineraryItem affected, Incident incident);
}
