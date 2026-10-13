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

    private Activity dependency(ActivityId id) {
        final var inTheSameRequest = built.get(id);
        return inTheSameRequest != null ? inTheSameRequest : Lookups.item(expedition, id).getActivity();
    }
}
