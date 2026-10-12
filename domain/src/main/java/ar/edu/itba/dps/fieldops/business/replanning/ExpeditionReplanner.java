package ar.edu.itba.dps.fieldops.business.replanning;

import ar.edu.itba.dps.fieldops.business.exceptions.InvalidScheduleException;
import ar.edu.itba.dps.fieldops.business.interfaces.replanning.ReplanReason;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.Resource;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionBuilder;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ReplannedActivity;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ResourceAssignment;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public class ExpeditionReplanner {

    private static final ExpeditionRepository PREVIEW_IDS = new PreviewIds();

    private final ExpeditionRepository repository;

    public ExpeditionReplanner(ExpeditionRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
    }

    public Expedition planAlternative(Expedition original, ItineraryItem affectedItem, ReplanReason reason) {
        requireArguments(original, affectedItem, reason);
        original.requireReplaceable();
        return draftOf(original, affectedItem, reason, repository);
    }

    /**
     * True when the alternative for this reason can be built from the itinerary as it is now. It does not look at
     * the status of the original: a proposal is made while the expedition is still in execution (D46).
     */
    public boolean canPlan(Expedition original, ItineraryItem affectedItem, ReplanReason reason) {
        requireArguments(original, affectedItem, reason);
        try {
            draftOf(original, affectedItem, reason, PREVIEW_IDS);
            return true;
        } catch (InvalidScheduleException unplannable) {
            return false;
        }
    }

    private void requireArguments(Expedition original, ItineraryItem affectedItem, ReplanReason reason) {
        Objects.requireNonNull(original, "original is required");
        Objects.requireNonNull(affectedItem, "affectedItem is required");
        Objects.requireNonNull(reason, "reason is required");
        requireOwnItem(original, affectedItem);
    }

    private Expedition draftOf(Expedition original, ItineraryItem affectedItem, ReplanReason reason, ExpeditionRepository ids) {
        final var carried = carriedItemsOf(original, affectedItem, reason);
        final var alternative = draftReplacing(original, reason, carried).build(ids);
        carried.values().forEach(item -> copyAssignments(item, alternative));
        return alternative;
    }

    private ExpeditionBuilder draftReplacing(Expedition original, ReplanReason reason, Map<ItineraryItem, CarriedItem> carried) {
        final var builder = sameGeneralDataAs(original)
                .withReplacedExpedition(original.getId())
                .withReplanReason(reason);
        original.getIncidents().forEach(builder::addInheritedIncident);
        original.getItinerary().planKeepingStructure(item -> planOf(item, carried)).forEach(builder::addNode);
        return builder;
    }

    /**
     * An item that comes back is planned again, even the affected one if it had already started (it is redone,
     * D47). Anything else that already finished stays closed.
     */
    private Optional<PlannedNode> planOf(ItineraryItem item, Map<ItineraryItem, CarriedItem> carried) {
        if (carried.containsKey(item)) {
            return Optional.of(PlannedNode.activity(item.getActivity(), carried.get(item).period()));
        }
        return item.isFinished() ? Optional.of(PlannedNode.finished(item)) : Optional.empty();
    }

    private ExpeditionBuilder sameGeneralDataAs(Expedition original) {
        final var builder = new ExpeditionBuilder()
                .withName(original.getName())
                .withPeriod(original.getPeriod())
                .withRestrictions(original.getRestrictions());
        original.getObjectives().forEach(builder::addObjective);
        original.getZones().forEach(builder::addZone);
        original.getResponsibles().forEach(builder::addResponsible);
        original.getGrantedPermits().forEach(builder::addPermit);
        return builder;
    }

    /**
     * What goes back to the alternative as a plan. What the reason does with the affected activity drags others
     * along: a later start moves later whatever runs after it or depends on it, and a cancellation drops whatever
     * depends on it (D46).
     */
    private Map<ItineraryItem, CarriedItem> carriedItemsOf(Expedition original, ItineraryItem affectedItem, ReplanReason reason) {
        final var itinerary = original.getItinerary();
        final var carried = new LinkedHashMap<ItineraryItem, CarriedItem>();
        itinerary.getItems().stream()
                .filter(item -> !item.isFinished() && !item.equals(affectedItem))
                .forEach(item -> carried.put(item, CarriedItem.unchanged(item)));
        reason.replan(affectedItem).ifPresentOrElse(replanned -> {
            carried.put(affectedItem, CarriedItem.replanned(affectedItem, replanned));
            final var shift = Duration.between(affectedItem.getScheduledPeriod().start(), replanned.period().start());
            if (shift.isPositive()) {
                itinerary.movingWith(affectedItem).forEach(item -> carried.put(requirePending(item, "moved"),
                        CarriedItem.unchanged(item).shiftedBy(shift)));
            }
        }, () -> itinerary.dependentsOf(affectedItem).forEach(item -> carried.remove(requirePending(item, "cancelled"))));
        return carried;
    }

    private void copyAssignments(CarriedItem carried, Expedition alternative) {
        final var source = carried.source();
        final var copy = alternative.getItinerary().itemFor(source.getActivity()).orElseThrow();
        source.getAssignedStaff().stream().filter(carried.keeps()).forEach(person -> alternative.assignStaff(copy, person));
        source.getAssignedEquipment().stream().filter(carried.keeps()).forEach(equipment -> alternative.assignEquipment(copy, equipment));
        source.getAssignedSupplies().stream().filter(carried.keeps()).forEach(supply -> alternative.assignSupply(copy, supply));
        carried.additions().forEach(addition -> addition.applyTo(alternative, copy));
    }

    private ItineraryItem requirePending(ItineraryItem item, String change) {
        if (item.isStarted()) {
            throw new InvalidScheduleException("%s already started and cannot be %s".formatted(item.getActivity().getName(), change));
        }
        return item;
    }

    private void requireOwnItem(Expedition original, ItineraryItem affectedItem) {
        if (!original.getItinerary().contains(affectedItem)) {
            throw new IllegalArgumentException("the affected item does not belong to the original expedition");
        }
    }

    private record CarriedItem(ItineraryItem source, TimePeriod period, Predicate<Resource> keeps,
                               List<ResourceAssignment> additions) {

        static CarriedItem unchanged(ItineraryItem item) {
            return new CarriedItem(item, item.getScheduledPeriod(), _ -> true, List.of());
        }

        static CarriedItem replanned(ItineraryItem item, ReplannedActivity replanned) {
            return new CarriedItem(item, replanned.period(), replanned.keeps(), replanned.additions());
        }

        CarriedItem shiftedBy(Duration shift) {
            return new CarriedItem(source, period.shiftedBy(shift), keeps, additions);
        }
    }

    /**
     * Hands out a throwaway id to check whether an alternative can be built. Nothing built with it is stored.
     */
    private static final class PreviewIds implements ExpeditionRepository {

        @Override
        public ExpeditionId nextId() {
            return new ExpeditionId("preview");
        }

        @Override
        public void add(Expedition expedition) {
            throw new UnsupportedOperationException("a preview is never stored");
        }

        @Override
        public Optional<Expedition> findById(ExpeditionId id) {
            return Optional.empty();
        }

        @Override
        public List<Expedition> findAll() {
            return List.of();
        }
    }
}
