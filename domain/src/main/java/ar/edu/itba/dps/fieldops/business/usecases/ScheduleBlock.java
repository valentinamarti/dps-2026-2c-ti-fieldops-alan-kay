package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.interfaces.itinerary.ItineraryNode;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;
import ar.edu.itba.dps.fieldops.business.rules.ActivityTypeRegistry;

import java.util.Objects;

public class ScheduleBlock {
    private final ExpeditionRepository repository;
    private final ZoneRepository zones;
    private final ActivityTypeRegistry types;

    public ScheduleBlock(ExpeditionRepository repository, ZoneRepository zones, ActivityTypeRegistry types) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
        this.zones = Objects.requireNonNull(zones, "zones are required");
        this.types = Objects.requireNonNull(types, "types are required");
    }

    public ItineraryNode schedule(ScheduleBlockCommand command) {
        final var expedition = Lookups.expedition(repository, command.expeditionId());
        final var block = Objects.requireNonNull(command.block(), "block is required");
        return expedition.schedule(block.plan(new PlannedActivities(expedition, zones, types)));
    }

    public ItineraryNode schedule(ExpeditionId expeditionId, PlannedNode block) {
        return Lookups.expedition(repository, expeditionId).schedule(block);
    }
}
