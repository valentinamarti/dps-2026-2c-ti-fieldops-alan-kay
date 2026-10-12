package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.interfaces.itinerary.ItineraryNode;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;

import java.util.Objects;

public class ScheduleBlock {

    private final ExpeditionRepository repository;

    public ScheduleBlock(ExpeditionRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
    }

    public ItineraryNode schedule(ScheduleBlockCommand command) {
        return Lookups.expedition(repository, command.expeditionId()).schedule(command.block());
    }
}
