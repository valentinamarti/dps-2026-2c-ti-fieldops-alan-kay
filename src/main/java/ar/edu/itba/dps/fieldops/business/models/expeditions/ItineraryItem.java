package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.exceptions.InvalidScheduleException;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.DepletableResource;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.Equipment;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class ItineraryItem {

    @Getter
    private final Activity activity;
    @Getter
    private final TimePeriod scheduledPeriod;
    private final Set<Person> assignedStaff = new LinkedHashSet<>();
    private final Set<Equipment> assignedEquipment = new LinkedHashSet<>();
    private final Set<DepletableResource> assignedSupplies = new LinkedHashSet<>();

    ItineraryItem(Activity activity, TimePeriod scheduledPeriod) {
        this.activity = Objects.requireNonNull(activity, "activity is required");
        this.scheduledPeriod = Objects.requireNonNull(scheduledPeriod, "scheduled period is required");
        if (!activity.getTimeWindow().contains(scheduledPeriod)) {
            throw new InvalidScheduleException("%s must be scheduled within its time window".formatted(activity.getName()));
        }
    }

    public boolean isFor(Activity activity) {
        return this.activity.equals(activity);
    }

    public boolean overlapsWith(ItineraryItem other) {
        return scheduledPeriod.overlapsWith(other.scheduledPeriod);
    }

    public Set<Person> getAssignedStaff() {
        return Collections.unmodifiableSet(assignedStaff);
    }

    public Set<Equipment> getAssignedEquipment() {
        return Collections.unmodifiableSet(assignedEquipment);
    }

    public Set<DepletableResource> getAssignedSupplies() {
        return Collections.unmodifiableSet(assignedSupplies);
    }

    boolean endsBeforeStartOf(ItineraryItem other) {
        return !scheduledPeriod.end().isAfter(other.scheduledPeriod.start());
    }

    void assignStaff(Person person) {
        assignedStaff.add(Objects.requireNonNull(person, "person is required"));
    }

    void assignEquipment(Equipment equipment) {
        assignedEquipment.add(Objects.requireNonNull(equipment, "equipment is required"));
    }

    void assignSupply(DepletableResource supply) {
        assignedSupplies.add(Objects.requireNonNull(supply, "supply is required"));
    }
}
