package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.NotFoundException;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;
import ar.edu.itba.dps.fieldops.business.rules.ActivityTypeRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds the activities that one request describes, which is the resolution {@link ScheduleActivity} and
 * {@link ScheduleBlock} share (D53, D54): the zone comes from the repository of zones, the rules from the
 * registry of types, and each dependency from the activities of the expedition being planned.
 *
 * <p>One instance serves one request, and that is what lets a leaf of a block depend on an earlier leaf of the
 * same block: while the tree is being built neither of the two is scheduled, so the itinerary cannot answer for
 * them yet, but this does.
 */
final class PlannedActivities {

    private final Expedition expedition;
    private final ZoneRepository zones;
    private final ActivityTypeRegistry types;
    private final Map<ActivityId, Activity> built = new LinkedHashMap<>();

    PlannedActivities(Expedition expedition, ZoneRepository zones, ActivityTypeRegistry types) {
        this.expedition = Objects.requireNonNull(expedition, "expedition is required");
        this.zones = Objects.requireNonNull(zones, "zones are required");
        this.types = Objects.requireNonNull(types, "types are required");
    }

    Activity of(PlannedActivityCommand command) {
        final var activity = new Activity(
                command.id(),
                command.name(),
                command.timeWindow(),
                Lookups.zone(zones, command.zoneId()),
                dependenciesOf(command),
                types.rulesFor(command.typeName(), command.parameters(), command.atNight()));
        built.put(activity.getId(), activity);
        return activity;
    }

    private List<Activity> dependenciesOf(PlannedActivityCommand command) {
        return DomainArguments.requireList(command.dependencyIds(), "dependencies").stream()
                .map(this::dependency)
                .toList();
    }

    /**
     * A dependency is an activity of the same expedition: one this request already built, which is how an
     * activity of a block depends on an earlier one of the same block (D40), or one that is already scheduled
     * in its itinerary.
     *
     * <p>Anything else fails with {@link NotFoundException}, so an id of another expedition, one nobody
     * scheduled and one of a leaf that comes later in the same tree all read the same way: from the expedition
     * being planned, at the moment this activity is built, they do not exist.
     */
    private Activity dependency(ActivityId id) {
        final var inTheSameRequest = built.get(id);
        return inTheSameRequest != null ? inTheSameRequest : Lookups.item(expedition, id).getActivity();
    }
}
