package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.exceptions.ExpeditionNotApprovableException;
import ar.edu.itba.dps.fieldops.business.exceptions.ExpeditionNotEditableException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidScheduleException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidStatusTransitionException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidTrackingException;
import ar.edu.itba.dps.fieldops.business.exceptions.ResourceUnavailableException;
import ar.edu.itba.dps.fieldops.business.exceptions.UnacceptedWarningException;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.DepletableResource;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.Equipment;
import ar.edu.itba.dps.fieldops.business.interfaces.validation.ExpeditionValidator;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.validation.AcceptedWarning;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Expedition {

    @Getter
    private final String id;
    @Getter
    private final String name;
    @Getter
    private final TimePeriod period;
    @Getter
    private final List<String> objectives;
    @Getter
    private final Set<Zone> zones;
    @Getter
    private final Set<Person> responsibles;
    @Getter
    private final ExpeditionRestrictions restrictions;
    @Getter
    private final Set<Permit> grantedPermits;
    @Getter
    private final Itinerary itinerary = new Itinerary();
    @Getter
    private ExpeditionStatus status = ExpeditionStatus.DRAFT;
    @Getter
    private List<AcceptedWarning> acceptedWarnings = List.of();

    Expedition(String id, String name, TimePeriod period, List<String> objectives, Set<Zone> zones,
               Set<Person> responsibles, ExpeditionRestrictions restrictions, Set<Permit> grantedPermits) {
        this.id = DomainArguments.requireText(id, "id");
        this.name = DomainArguments.requireText(name, "name");
        this.period = Objects.requireNonNull(period, "period is required");
        this.objectives = DomainArguments.requireNonEmptyList(objectives, "objectives");
        this.objectives.forEach(objective -> DomainArguments.requireText(objective, "objective"));
        this.zones = DomainArguments.requireNonEmptySet(zones, "zones");
        this.responsibles = DomainArguments.requireNonEmptySet(responsibles, "responsibles");
        this.restrictions = Objects.requireNonNull(restrictions, "restrictions are required");
        this.grantedPermits = DomainArguments.requireSet(grantedPermits, "grantedPermits");
    }

    public ItineraryItem schedule(Activity activity, TimePeriod scheduledPeriod) {
        requireEditable();
        requireOwnZone(activity);
        requireWithinPeriod(activity, scheduledPeriod);
        return itinerary.add(activity, scheduledPeriod);
    }

    public void assignStaff(ItineraryItem item, Person person) {
        editable(item).assignStaff(person);
    }

    public void assignEquipment(ItineraryItem item, Equipment equipment) {
        editable(item).assignEquipment(equipment);
    }

    public void assignSupply(ItineraryItem item, DepletableResource supply) {
        editable(item).assignSupply(supply);
    }

    public Set<Person> participants() {
        return itinerary.getItems().stream()
                .flatMap(item -> item.getAssignedStaff().stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    public void submitForReview() {
        transitionTo(ExpeditionStatus.IN_REVIEW);
    }

    public void returnToDraft() {
        transitionTo(ExpeditionStatus.DRAFT);
    }

    public void approve(ExpeditionValidator validator, List<AcceptedWarning> acceptances) {
        requireCanTransitionTo(ExpeditionStatus.APPROVED);
        final var result = validator.validate(this);
        if (!result.canBeApproved()) {
            throw new ExpeditionNotApprovableException(result.criticals());
        }
        final var accepted = result.warnings().stream().map(warning -> acceptanceOf(warning, acceptances)).toList();
        reserveAssignedResources();
        acceptedWarnings = accepted;
        status = ExpeditionStatus.APPROVED;
    }

    public void start() {
        transitionTo(ExpeditionStatus.IN_EXECUTION);
    }

    public void suspend() {
        transitionTo(ExpeditionStatus.SUSPENDED);
    }

    public void resume() {
        transitionTo(ExpeditionStatus.IN_EXECUTION);
    }

    public void finish() {
        transitionTo(ExpeditionStatus.FINISHED);
    }

    public void startActivity(ItineraryItem item, LocalDateTime at) {
        inExecution(item).start(at);
    }

    /**
     * Closing an activity consumes what it actually used, so the supplies left reflect the field, not the plan.
     */
    public void finishActivity(ItineraryItem item, LocalDateTime at, String result) {
        final var tracked = inExecution(item);
        tracked.startedTracking().finish(at, result);
        consumeAssignedSupplies(tracked);
    }

    public void recordObservation(ItineraryItem item, Observation observation) {
        inExecution(item).startedTracking().record(observation);
    }

    public void reportIncident(ItineraryItem item, Incident incident) {
        inExecution(item).startedTracking().record(incident);
    }

    private void consumeAssignedSupplies(ItineraryItem item) {
        item.getActivity().requiredSupplies().forEach(requirement ->
                item.supplyFor(requirement).ifPresent(supply -> supply.consume(requirement.quantity())));
    }

    private ItineraryItem inExecution(ItineraryItem item) {
        if (status != ExpeditionStatus.IN_EXECUTION) {
            throw new InvalidTrackingException(
                    "activities can only be tracked while the expedition is in execution, but it is %s".formatted(status));
        }
        return requireOwnItem(item);
    }

    private AcceptedWarning acceptanceOf(ValidationResult warning, List<AcceptedWarning> acceptances) {
        return acceptances.stream()
                .filter(acceptance -> acceptance.covers(warning) && responsibles.contains(acceptance.responsible()))
                .findFirst()
                .orElseThrow(() -> new UnacceptedWarningException(warning));
    }

    /**
     * Checks every assigned resource before booking any of them, so a resource taken by another
     * expedition since the validation ran leaves this one untouched instead of half reserved.
     */
    private void reserveAssignedResources() {
        itinerary.getItems().forEach(this::requireAssignedResourcesAvailable);
        itinerary.getItems().forEach(item ->
                item.getAssignedReusableResources().forEach(resource -> resource.reserve(item.getScheduledPeriod())));
    }

    private void requireAssignedResourcesAvailable(ItineraryItem item) {
        for (final var resource : item.getAssignedReusableResources()) {
            if (!resource.isAvailableDuring(item.getScheduledPeriod())) {
                throw new ResourceUnavailableException(item.getScheduledPeriod());
            }
        }
    }

    private void transitionTo(ExpeditionStatus next) {
        requireCanTransitionTo(next);
        status = next;
    }

    private void requireCanTransitionTo(ExpeditionStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new InvalidStatusTransitionException(status, next);
        }
    }

    private ItineraryItem editable(ItineraryItem item) {
        requireEditable();
        return requireOwnItem(item);
    }

    private ItineraryItem requireOwnItem(ItineraryItem item) {
        if (!itinerary.contains(item)) {
            throw new IllegalArgumentException("the item does not belong to this expedition");
        }
        return item;
    }

    private void requireEditable() {
        if (status != ExpeditionStatus.DRAFT) {
            throw new ExpeditionNotEditableException(status);
        }
    }

    private void requireOwnZone(Activity activity) {
        if (!zones.contains(activity.getZone())) {
            throw new InvalidScheduleException("%s takes place in %s, which is not a zone of this expedition"
                    .formatted(activity.getName(), activity.getZone().getName()));
        }
    }

    private void requireWithinPeriod(Activity activity, TimePeriod scheduledPeriod) {
        if (!period.contains(scheduledPeriod)) {
            throw new InvalidScheduleException("%s is scheduled outside the expedition period".formatted(activity.getName()));
        }
    }
}
