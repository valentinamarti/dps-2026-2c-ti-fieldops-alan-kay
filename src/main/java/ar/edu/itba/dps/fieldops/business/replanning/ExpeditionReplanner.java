package ar.edu.itba.dps.fieldops.business.replanning;

import ar.edu.itba.dps.fieldops.business.interfaces.replanning.ReplanReason;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.Resource;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionBuilder;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ExpeditionReplanner {

    public Expedition planAlternative(Expedition original, ExpeditionId alternativeId, ItineraryItem affectedItem,
                                      ReplanReason reason) {
        Objects.requireNonNull(original, "original is required");
        Objects.requireNonNull(affectedItem, "affectedItem is required");
        Objects.requireNonNull(reason, "reason is required");
        original.requireReplaceable();
        requireOwnItem(original, affectedItem);
        requirePending(affectedItem);

        final var builder = new ExpeditionBuilder()
                .withName(original.getName())
                .withPeriod(original.getPeriod())
                .withRestrictions(original.getRestrictions())
                .withReplacedExpedition(original.getId())
                .withReplanReason(reason);
        original.getObjectives().forEach(builder::addObjective);
        original.getZones().forEach(builder::addZone);
        original.getResponsibles().forEach(builder::addResponsible);
        original.getGrantedPermits().forEach(builder::addPermit);
        finishedItemsOf(original).forEach(builder::addFinishedItem);
        final var carried = carriedItemsOf(original, affectedItem, reason);
        carried.forEach(item -> builder.addActivity(item.source().getActivity(), item.period()));

        final var alternative = builder.build(alternativeId);
        carried.forEach(item -> copyAssignments(item, alternative));
        return alternative;
    }

    private List<CarriedItem> carriedItemsOf(Expedition original, ItineraryItem affectedItem, ReplanReason reason) {
        final var pending = pendingItemsOf(original, affectedItem)
                .map(item -> new CarriedItem(item, item.getScheduledPeriod(), _ -> true));
        final var affected = reason.replan(affectedItem).stream()
                .map(replanned -> new CarriedItem(affectedItem, replanned.period(), replanned.keeps()));
        return Stream.concat(pending, affected)
                .sorted(Comparator.comparing(item -> item.period().start()))
                .toList();
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

    private Stream<ItineraryItem> finishedItemsOf(Expedition original) {
        return original.getItinerary().getItems().stream().filter(ItineraryItem::isFinished);
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
