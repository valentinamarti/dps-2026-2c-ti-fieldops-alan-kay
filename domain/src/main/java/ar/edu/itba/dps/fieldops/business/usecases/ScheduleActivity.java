package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;
import ar.edu.itba.dps.fieldops.business.rules.ActivityTypeRegistry;

import java.util.List;
import java.util.Objects;

/**
 * Schedules an activity in an expedition. The command brings ids and text, so this is the use case that
 * assembles the {@link Activity}: it resolves the zone in the repository, the dependencies in the itinerary of
 * the same expedition and the rules in the registry of types (D53).
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
        final var activity = new Activity(
                command.activityId(),
                command.name(),
                command.timeWindow(),
                Lookups.zone(zones, command.zoneId()),
                dependenciesIn(expedition, command.dependencyIds()),
                types.rulesFor(command.typeName(), command.parameters(), command.atNight()));
        return expedition.schedule(activity, command.scheduledPeriod());
    }

    public ItineraryItem schedule(ExpeditionId expeditionId, Activity activity, TimePeriod scheduledPeriod) {
        return Lookups.expedition(repository, expeditionId).schedule(activity, scheduledPeriod);
    }

    /**
     * A dependency is an activity of the same expedition that is already scheduled: it is looked up in its
     * itinerary, so an id that nobody scheduled and an id that belongs to another expedition fail the same
     * way, with {@code NotFoundException}. It could not be otherwise: the expedition is the one that checks
     * that a dependency finishes before the activity starts (D21), and it only sees its own itinerary.
     */
    private List<Activity> dependenciesIn(Expedition expedition, List<ActivityId> dependencyIds) {
        return DomainArguments.requireList(dependencyIds, "dependencies").stream()
                .map(id -> Lookups.item(expedition, id).getActivity())
                .toList();
    }
}
