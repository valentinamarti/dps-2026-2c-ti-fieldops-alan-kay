package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;

import java.util.Objects;

public class ScheduleActivity {

    private final ExpeditionRepository repository;

    public ScheduleActivity(ExpeditionRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
    }

    public ItineraryItem schedule(ScheduleActivityCommand command) {
        return Lookups.expedition(repository, command.expeditionId()).schedule(command.activity(), command.scheduledPeriod());
    }
}
