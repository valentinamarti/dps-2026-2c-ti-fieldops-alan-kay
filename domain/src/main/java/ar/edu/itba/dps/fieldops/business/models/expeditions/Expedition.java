package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.exceptions.ActivitiesInProgressException;
import ar.edu.itba.dps.fieldops.business.exceptions.ExpeditionNotApprovableException;
import ar.edu.itba.dps.fieldops.business.exceptions.ExpeditionNotEditableException;
import ar.edu.itba.dps.fieldops.business.exceptions.InsufficientStockException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidScheduleException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidStatusTransitionException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidTrackingException;
import ar.edu.itba.dps.fieldops.business.exceptions.NotAResponsibleException;
import ar.edu.itba.dps.fieldops.business.exceptions.UnacceptedWarningException;
import ar.edu.itba.dps.fieldops.business.interfaces.itinerary.ItineraryNode;
import ar.edu.itba.dps.fieldops.business.interfaces.replanning.ReplanReason;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.DepletableResource;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.Equipment;
import ar.edu.itba.dps.fieldops.business.interfaces.validation.ValidationRule;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.validation.AcceptedWarning;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.validations.ApprovalPolicy;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Expedition {

    @Getter
    private final ExpeditionId id;
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
    private final List<Incident> incidents = new ArrayList<>();
    private final ExpeditionId replacedExpedition;
    private final ReplanReason replanReason;
    private ExpeditionId replacement;

    Expedition(ExpeditionId id, String name, TimePeriod period, List<String> objectives, Set<Zone> zones,
               Set<Person> responsibles, ExpeditionRestrictions restrictions, Set<Permit> grantedPermits,
               ExpeditionId replacedExpedition, ReplanReason replanReason, List<Incident> inheritedIncidents) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.name = DomainArguments.requireText(name, "name");
        this.period = Objects.requireNonNull(period, "period is required");
        this.objectives = DomainArguments.requireNonEmptyList(objectives, "objectives");
        this.objectives.forEach(objective -> DomainArguments.requireText(objective, "objective"));
        this.zones = DomainArguments.requireNonEmptySet(zones, "zones");
        this.responsibles = DomainArguments.requireNonEmptySet(responsibles, "responsibles");
        this.restrictions = Objects.requireNonNull(restrictions, "restrictions are required");
        this.grantedPermits = DomainArguments.requireSet(grantedPermits, "grantedPermits");
        this.replacedExpedition = replacedExpedition;
        this.replanReason = replanReason;
        this.incidents.addAll(DomainArguments.requireList(inheritedIncidents, "inheritedIncidents"));
    }

    public ItineraryItem schedule(Activity activity, TimePeriod scheduledPeriod) {
        requireEditable();
        requireOwnZone(activity);
        requireWithinPeriod(activity, scheduledPeriod);
        return itinerary.add(activity, scheduledPeriod);
    }

    public ItineraryNode schedule(PlannedNode plan) {
        requireEditable();
        return itinerary.add(plan.nodeIn(this));
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

    public void approve(List<ValidationRule> additionalRules, List<AcceptedWarning> acceptances) {
        requireCanTransitionTo(ExpeditionStatus.APPROVED);
        final var result = new ApprovalPolicy(additionalRules).validate(this);
        if (!result.canBeApproved()) {
            throw new ExpeditionNotApprovableException(result.criticals());
        }
        acceptedWarnings = result.warnings().stream().map(warning -> acceptanceOf(warning, acceptances)).toList();
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
        requireNoActivityInProgress();
        transitionTo(ExpeditionStatus.FINISHED);
    }

    public Optional<ExpeditionId> getReplacedExpedition() {
        return Optional.ofNullable(replacedExpedition);
    }

    public Set<ExpeditionId> reservationsItTakesOver() {
        return getReplacedExpedition().map(Set::of).orElse(Set.of());
    }

    public Optional<ReplanReason> getReplanReason() {
        return Optional.ofNullable(replanReason);
    }

    public Optional<ExpeditionId> getReplacement() {
        return Optional.ofNullable(replacement);
    }

    public void markReplacedBy(ExpeditionId alternative) {
        requireReplaceable();
        replacement = Objects.requireNonNull(alternative, "alternative is required");
        status = ExpeditionStatus.REPLACED;
    }

    public void requireReplaceable() {
        requireNoActivityInProgress();
        requireCanTransitionTo(ExpeditionStatus.REPLACED);
    }

    /**
     * Leaves the expedition ready to be replaced: one in execution is suspended first, so the field stops
     * following a plan that is about to change (D47). Nothing changes if it cannot be replaced.
     */
    public void suspendToReplan() {
        requireNoActivityInProgress();
        if (status.canTransitionTo(ExpeditionStatus.SUSPENDED)) {
            transitionTo(ExpeditionStatus.SUSPENDED);
        }
        requireCanTransitionTo(ExpeditionStatus.REPLACED);
    }

    public void requireResponsible(Person person) {
        if (!responsibles.contains(person)) {
            throw new NotAResponsibleException(person.getId(), id);
        }
    }

    public void startActivity(ItineraryItem item, LocalDateTime at) {
        inExecution(item).start(at);
    }

    /**
     * Closing an activity consumes what it actually used, so the supplies left reflect the field, not the plan.
     */
    public void finishActivity(ItineraryItem item, LocalDateTime at, String result) {
        final var tracking = inExecution(item).startedTracking();
        tracking.requireCanFinish(at, result);
        consumeAssignedSupplies(item);
        tracking.finish(at, result);
    }

    public void recordObservation(ItineraryItem item, Observation observation) {
        inExecution(item).startedTracking().record(observation);
    }

    /**
     * Registers what went wrong with an activity that has not finished yet, pending or in progress. The incident
     * may name a resource, which has to be one assigned to that activity.
     */
    public Incident registerIncident(ItineraryItem item, IncidentReport report) {
        Objects.requireNonNull(report, "report is required");
        final var affected = inExecution(item);
        if (affected.isFinished()) {
            throw new InvalidTrackingException("%s already finished".formatted(affected.getActivity().getName()));
        }
        report.resource().ifPresent(resource -> requireAssigned(affected, resource));
        final var incident = Incident.registered(new IncidentId(id, incidents.size() + 1), affected.getActivity().getId(), report);
        incidents.add(incident);
        return incident;
    }

    /**
     * The incidents of the whole trip, in the order they were registered. An alternative starts with the ones of
     * the expedition it replaces.
     */
    public List<Incident> getIncidents() {
        return Collections.unmodifiableList(incidents);
    }

    private void requireAssigned(ItineraryItem item, ResourceId resource) {
        final var assigned = Stream.concat(item.getAssignedReusableResources().stream(), item.getAssignedSupplies().stream())
                .anyMatch(candidate -> candidate.getId().equals(resource));
        if (!assigned) {
            throw new InvalidTrackingException("%s is not assigned to %s".formatted(resource, item.getActivity().getName()));
        }
    }

    private void consumeAssignedSupplies(ItineraryItem item) {
        final var demands = item.supplyDemands();
        demands.forEach(this::requireConsumable);
        demands.forEach(demand -> demand.supply().consume(id, demand.total()));
    }

    private void requireConsumable(SupplyDemand demand) {
        if (!demand.supply().canConsume(id, demand.total())) {
            throw new InsufficientStockException(demand.supply().getId(), demand.total());
        }
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

    private void requireNoActivityInProgress() {
        final var inProgress = itinerary.getItems().stream()
                .filter(ItineraryItem::isInProgress)
                .map(item -> item.getActivity().getName())
                .toList();
        if (!inProgress.isEmpty()) {
            throw new ActivitiesInProgressException(inProgress);
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

    ItineraryItem plannedItem(Activity activity, TimePeriod scheduledPeriod) {
        requireOwnZone(activity);
        requireWithinPeriod(activity, scheduledPeriod);
        return new ItineraryItem(activity, scheduledPeriod);
    }

    private ItineraryItem editable(ItineraryItem item) {
        requireEditable();
        requireOwnItem(item);
        if (item.isStarted()) {
            throw new InvalidScheduleException("%s already happened and cannot change".formatted(item.getActivity().getName()));
        }
        return item;
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
