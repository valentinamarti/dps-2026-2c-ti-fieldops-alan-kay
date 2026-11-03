package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.interfaces.resources.DepletableResource;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.Equipment;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ItemSnapshot(Activity activity, TimePeriod scheduledPeriod, List<Person> staff, List<Equipment> equipment,
                           List<DepletableResource> supplies, Optional<TrackingSnapshot> tracking) implements NodeSnapshot {

    public ItemSnapshot {
        Objects.requireNonNull(activity, "activity is required");
        Objects.requireNonNull(scheduledPeriod, "scheduledPeriod is required");
        staff = List.copyOf(staff);
        equipment = List.copyOf(equipment);
        supplies = List.copyOf(supplies);
        Objects.requireNonNull(tracking, "tracking is required");
    }
}
