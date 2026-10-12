package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.interfaces.itinerary.ItineraryNode;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;
import ar.edu.itba.dps.fieldops.business.rules.ActivityTypeRegistry;

import java.util.Objects;

/**
 * Schedules a block in an expedition. The command brings a tree of descriptions, so this turns it into the
 * {@link PlannedNode} the expedition agrees to schedule: each leaf is resolved by {@link PlannedActivities},
 * the same resolution the schedule of a single activity uses (D53, D54), and the block goes in whole, so it is
 * the expedition that applies to it the rules of any plan (D39, D40).
 *
 * <p>The variant that receives the {@code PlannedNode} already built stays, for the same reason as the one of
 * {@link ScheduleActivity}.
 */
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
