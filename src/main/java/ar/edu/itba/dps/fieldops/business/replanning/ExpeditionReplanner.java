package ar.edu.itba.dps.fieldops.business.replanning;

import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionBuilder;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;

import java.util.Objects;
import java.util.stream.Stream;

public class ExpeditionReplanner {

    public Expedition planAlternative(Expedition original, ExpeditionId alternativeId, ItineraryItem affectedItem) {
        Objects.requireNonNull(original, "original is required");
        Objects.requireNonNull(affectedItem, "affectedItem is required");
        original.requireReplaceable();
        requireOwnItem(original, affectedItem);

        final var builder = new ExpeditionBuilder()
                .withName(original.getName())
                .withPeriod(original.getPeriod())
                .withRestrictions(original.getRestrictions());
        original.getObjectives().forEach(builder::addObjective);
        original.getZones().forEach(builder::addZone);
        original.getResponsibles().forEach(builder::addResponsible);
        original.getGrantedPermits().forEach(builder::addPermit);
        finishedItemsOf(original).forEach(builder::addFinishedItem);
        pendingItemsOf(original, affectedItem)
                .forEach(item -> builder.addActivity(item.getActivity(), item.getScheduledPeriod()));

        final var alternative = builder.build(alternativeId);
        copyAssignments(original, alternative, affectedItem);
        return alternative;
    }

    private void copyAssignments(Expedition original, Expedition alternative, ItineraryItem affectedItem) {
        pendingItemsOf(original, affectedItem).forEach(item -> {
            final var copy = alternative.getItinerary().itemFor(item.getActivity()).orElseThrow();
            item.getAssignedStaff().forEach(person -> alternative.assignStaff(copy, person));
            item.getAssignedEquipment().forEach(equipment -> alternative.assignEquipment(copy, equipment));
            item.getAssignedSupplies().forEach(supply -> alternative.assignSupply(copy, supply));
        });
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
}
