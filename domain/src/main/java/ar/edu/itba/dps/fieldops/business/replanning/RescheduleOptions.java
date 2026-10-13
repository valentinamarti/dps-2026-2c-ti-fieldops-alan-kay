package ar.edu.itba.dps.fieldops.business.replanning;

import ar.edu.itba.dps.fieldops.business.interfaces.replanning.ReplanOptionGenerator;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanOption;

import java.util.List;

public class RescheduleOptions implements ReplanOptionGenerator {
    @Override
    public List<ReplanOption> optionsFor(Expedition expedition, ItineraryItem affected, Incident incident) {
        return incident.expectedDelay()
                .map(delay -> new ReplanOption(new Delay(delay), OptionSummaries.withOthers(
                        "Run %s %s later".formatted(affected.getActivity().getName(), OptionSummaries.duration(delay)),
                        "together with", expedition.getItinerary().movingWith(affected))))
                .stream()
                .toList();
    }
}
