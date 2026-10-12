package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.exceptions.InvalidScheduleException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidTrackingException;
import ar.edu.itba.dps.fieldops.business.interfaces.itinerary.ItineraryNode;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.DepletableResource;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.Equipment;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.ReusableResource;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class ItineraryItem implements ItineraryNode {

    @Getter
    private final Activity activity;
    @Getter
    private final TimePeriod scheduledPeriod;
    private final Set<Person> assignedStaff = new LinkedHashSet<>();
    private final Set<Equipment> assignedEquipment = new LinkedHashSet<>();
    private final Set<DepletableResource> assignedSupplies = new LinkedHashSet<>();
    private ActivityTracking tracking;

    ItineraryItem(Activity activity, TimePeriod scheduledPeriod) {
        this.activity = Objects.requireNonNull(activity, "activity is required");
        this.scheduledPeriod = Objects.requireNonNull(scheduledPeriod, "scheduled period is required");
        if (!activity.getTimeWindow().contains(scheduledPeriod)) {
            throw new InvalidScheduleException("%s must be scheduled within its time window".formatted(activity.getName()));
        }
    }

    static ItineraryItem finishedCopyOf(ItineraryItem finished) {
        final var copy = new ItineraryItem(finished.activity, finished.scheduledPeriod);
        copy.assignedStaff.addAll(finished.assignedStaff);
        copy.assignedEquipment.addAll(finished.assignedEquipment);
        copy.assignedSupplies.addAll(finished.assignedSupplies);
        copy.tracking = finished.finishedTracking().copy();
        return copy;
    }

    public boolean isFor(Activity activity) {
        return this.activity.equals(activity);
    }

    public boolean overlapsWith(ItineraryItem other) {
        return scheduledPeriod.overlapsWith(other.scheduledPeriod);
    }

    @Override
    public Duration estimatedDuration() {
        return activity.estimatedDuration();
    }

    @Override
    public RiskLevel estimatedRisk() {
        return activity.estimatedRisk();
    }

    @Override
    public List<DepletableRequirement> requiredSupplies() {
        return activity.requiredSupplies();
    }

    @Override
    public List<ItineraryItem> leaves() {
        return List.of(this);
    }

    @Override
    public boolean runsInParallel(ItineraryItem first, ItineraryItem second) {
        return false;
    }

    @Override
    public List<ItineraryItem> itemsAfter(ItineraryItem item) {
        return List.of();
    }

    @Override
    public Optional<PlannedNode> planKeepingStructure(Function<ItineraryItem, Optional<PlannedNode>> itemPlans) {
        return itemPlans.apply(this);
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

    public Set<ReusableResource> getAssignedReusableResources() {
        final var resources = new LinkedHashSet<ReusableResource>(assignedStaff);
        resources.addAll(assignedEquipment);
        return Collections.unmodifiableSet(resources);
    }

    /**
     * The supply assigned to this item that covers a requirement: the first one of its category.
     */
    public Optional<DepletableResource> supplyFor(DepletableRequirement requirement) {
        return assignedSupplies.stream().filter(requirement::accepts).findFirst();
    }

    public List<SupplyDemand> supplyDemands() {
        return SupplyDemand.combine(activity.requiredSupplies().stream().flatMap(requirement ->
                supplyFor(requirement).stream().map(supply -> new SupplyDemand(supply, List.of(requirement.quantity())))));
    }

    public Optional<ActivityTracking> getTracking() {
        return Optional.ofNullable(tracking);
    }

    public boolean isStarted() {
        return tracking != null;
    }

    @Override
    public boolean isFinished() {
        return tracking != null && tracking.isFinished();
    }

    public boolean isInProgress() {
        return isStarted() && !isFinished();
    }

    /**
     * True when the activity did not run inside the period it was scheduled for.
     */
    public boolean startedOutOfSchedule() {
        return isStarted() && !scheduledPeriod.includes(tracking.getStartedAt());
    }

    void start(LocalDateTime at) {
        if (isStarted()) {
            throw new InvalidTrackingException("%s had already started".formatted(activity.getName()));
        }
        tracking = new ActivityTracking(at);
    }

    ActivityTracking startedTracking() {
        if (!isStarted()) {
            throw new InvalidTrackingException("%s has not started yet".formatted(activity.getName()));
        }
        return tracking;
    }

    private ActivityTracking finishedTracking() {
        if (!isFinished()) {
            throw new InvalidTrackingException("%s has not finished yet".formatted(activity.getName()));
        }
        return tracking;
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