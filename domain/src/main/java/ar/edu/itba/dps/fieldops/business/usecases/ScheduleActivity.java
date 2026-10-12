package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;
import ar.edu.itba.dps.fieldops.business.rules.ActivityTypeRegistry;

import java.util.Objects;

/**
 * Schedules an activity in an expedition. The command brings ids and text, so the {@link Activity} is
 * assembled by {@link PlannedActivities}, the same resolution the schedule of a block uses for each of its
 * leaves (D53, D54).
 *
 * <p>The variant that receives the {@code Activity} already built stays, like the ones of D38: a caller that
 * is not an adapter (the tests, the builder of an expedition) already has the object and does not need the
 * registry to get it.
 */
public class ScheduleActivity {

    private final ExpeditionRepository repository;
    private final ZoneRepository zones;
    private final ActivityTypeRegistry types;

    public ScheduleActivity(ExpeditionRepository repository, ZoneRepository zones, ActivityTypeRegistry types) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
        this.zones = Objects.requireNonNull(zones, "zones are required");
        this.types = Objects.requireNonNull(types, "types are required");
    }

    public ItineraryItem schedule(ScheduleActivityCommand command) {
        final var expedition = Lookups.expedition(repository, command.expeditionId());
        final var planned = command.activity();
        final var activity = new PlannedActivities(expedition, zones, types).of(planned);
        return expedition.schedule(activity, planned.scheduledPeriod());
    }

    public ItineraryItem schedule(ExpeditionId expeditionId, Activity activity, TimePeriod scheduledPeriod) {
        return Lookups.expedition(repository, expeditionId).schedule(activity, scheduledPeriod);
    }
}
