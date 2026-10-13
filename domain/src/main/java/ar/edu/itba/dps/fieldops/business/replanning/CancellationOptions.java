package ar.edu.itba.dps.fieldops.business.replanning;

import ar.edu.itba.dps.fieldops.business.interfaces.replanning.ReplanOptionGenerator;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanOption;

import java.util.List;

public class CancellationOptions implements ReplanOptionGenerator {
    @Override
    public List<ReplanOption> optionsFor(Expedition expedition, ItineraryItem affected, Incident incident) {
        return List.of(new ReplanOption(new Cancellation(), OptionSummaries.withOthers(
                "Cancel %s".formatted(affected.getActivity().getName()),
                "and what depends on it:", expedition.getItinerary().dependentsOf(affected))));
    }
}
