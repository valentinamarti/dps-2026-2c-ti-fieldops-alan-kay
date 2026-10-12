package ar.edu.itba.dps.fieldops.business.replanning;

import ar.edu.itba.dps.fieldops.business.interfaces.replanning.ReplanReason;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.Resource;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionBuilder;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ExpeditionReplanner {

    private final ExpeditionRepository repository;

    public ExpeditionReplanner(ExpeditionRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
    }

    public Expedition planAlternative(Expedition original, ItineraryItem affectedItem, ReplanReason reason) {
        requireReplannable(original, affectedItem, reason);
        final var carried = carriedItemsOf(original, affectedItem, reason);
        final var alternative = draftReplacing(original, reason, carried).build(repository);
        carried.forEach(item -> copyAssignments(item, alternative));
        return alternative;
    }

    private void requireReplannable(Expedition original, ItineraryItem affectedItem, ReplanReason reason) {
        Objects.requireNonNull(original, "original is required");
        Objects.requireNonNull(affectedItem, "affectedItem is required");
        Objects.requireNonNull(reason, "reason is required");
        original.requireReplaceable();
        requireOwnItem(original, affectedItem);
        requirePending(affectedItem);
    }

    private ExpeditionBuilder draftReplacing(Expedition original, ReplanReason reason, List<CarriedItem> carried) {
        final var builder = sameGeneralDataAs(original)
                .withReplacedExpedition(original.getId())
                .withReplanReason(reason);
        original.getItinerary().planKeepingStructure(item -> planOf(item, carried)).forEach(builder::addNode);
        return builder;
    }

    private Optional<PlannedNode> planOf(ItineraryItem item, List<CarriedItem> carried) {
        if (item.isFinished()) {
            return Optional.of(PlannedNode.finished(item));
        }
        return carried.stream()
                .filter(candidate -> candidate.source().equals(item))
                .findFirst()
                .map(candidate -> PlannedNode.activity(item.getActivity(), candidate.period()));
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

    private List<CarriedItem> carriedItemsOf(Expedition original, ItineraryItem affectedItem, ReplanReason reason) {
        final var pending = pendingItemsOf(original, affectedItem)
                .map(item -> new CarriedItem(item, item.getScheduledPeriod(), _ -> true));
        final var affected = reason.replan(affectedItem).stream()
                .map(replanned -> new CarriedItem(affectedItem, replanned.period(), replanned.keeps()));
        return Stream.concat(pending, affected).toList();
    }

    private void copyAssignments(CarriedItem carried, Expedition alternative) {
        final var source = carried.source();
        final var copy = alternative.getItinerary().itemFor(source.getActivity()).orElseThrow();
        source.getAssignedStaff().stream().filter(carried.keeps()).forEach(person -> alternative.assignStaff(copy, person));
        source.getAssignedEquipment().stream().filter(carried.keeps()).forEach(equipment -> alternative.assignEquipment(copy, equipment));
        source.getAssignedSupplies().stream().filter(carried.keeps()).forEach(supply -> alternative.assignSupply(copy, supply));
    }

    private Stream<ItineraryItem> pendingItemsOf(Expedition original, ItineraryItem affectedItem) {
        return original.getItinerary().getItems().stream()
                .filter(item -> !item.equals(affectedItem) && !item.isStarted());
    }

    private void requireOwnItem(Expedition original, ItineraryItem affectedItem) {
        if (!original.getItinerary().contains(affectedItem)) {
            throw new IllegalArgumentException("the affected item does not belong to the original expedition");
        }
    }

    private void requirePending(ItineraryItem affectedItem) {
        if (affectedItem.isStarted()) {
            throw new IllegalArgumentException("%s already happened and cannot be replanned"
                    .formatted(affectedItem.getActivity().getName()));
        }
    }

    private record CarriedItem(ItineraryItem source, TimePeriod period, Predicate<Resource> keeps) {
    }
}
