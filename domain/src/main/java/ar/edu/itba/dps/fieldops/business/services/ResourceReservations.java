package ar.edu.itba.dps.fieldops.business.services;

import ar.edu.itba.dps.fieldops.business.exceptions.InsufficientStockException;
import ar.edu.itba.dps.fieldops.business.exceptions.ResourceUnavailableException;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.ReusableResource;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.expeditions.SupplyDemand;
import ar.edu.itba.dps.fieldops.business.models.resources.Reservation;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ResourceReservations {

    public void requireReservable(Expedition expedition) {
        requireReservable(plannedReservationsOf(expedition), reservableDemandsOf(expedition), expedition.reservationsItTakesOver());
    }

    public void reserveFor(Expedition expedition) {
        final var plannedReservations = plannedReservationsOf(expedition);
        final var demands = reservableDemandsOf(expedition);
        requireReservable(plannedReservations, demands, expedition.reservationsItTakesOver());
        plannedReservations.forEach(PlannedReservation::book);
        demands.forEach(demand -> demand.supply().reserve(expedition.getId(), demand.total()));
    }

    public void releaseFor(Expedition expedition) {
        final var items = expedition.getItinerary().getItems();
        items.stream()
                .flatMap(item -> item.getAssignedReusableResources().stream())
                .distinct()
                .forEach(resource -> resource.releaseAll(expedition.getId()));
        items.stream()
                .flatMap(item -> item.getAssignedSupplies().stream())
                .distinct()
                .forEach(supply -> supply.releaseAll(expedition.getId()));
    }

    private void requireReservable(List<PlannedReservation> plannedReservations, List<SupplyDemand> demands,
                                   Set<ExpeditionId> ignoredOwners) {
        plannedReservations.forEach(plannedReservation -> requireFree(plannedReservation, ignoredOwners));
        requireNoClashesBetween(plannedReservations);
        demands.forEach(demand -> requireInStock(demand, ignoredOwners));
    }

    private List<PlannedReservation> plannedReservationsOf(Expedition expedition) {
        return expedition.getItinerary().pendingItems().stream()
                .flatMap(item -> plannedReservationsOf(expedition, item))
                .toList();
    }

    private Stream<PlannedReservation> plannedReservationsOf(Expedition expedition, ItineraryItem item) {
        final var reservation = new Reservation(expedition.getId(), item.getScheduledPeriod());
        return item.getAssignedReusableResources().stream().map(resource -> new PlannedReservation(resource, reservation));
    }

    private List<SupplyDemand> reservableDemandsOf(Expedition expedition) {
        return expedition.getItinerary().supplyDemands().stream()
                .filter(SupplyDemand::isInSupplyUnit)
                .toList();
    }

    private void requireFree(PlannedReservation plannedReservation, Set<ExpeditionId> ignoredOwners) {
        if (!plannedReservation.isFree(ignoredOwners)) {
            throw new ResourceUnavailableException(plannedReservation.reservation().period());
        }
    }

    private void requireNoClashesBetween(List<PlannedReservation> plannedReservations) {
        IntStream.range(0, plannedReservations.size()).forEach(i ->
                plannedReservations.subList(i + 1, plannedReservations.size()).stream()
                        .filter(plannedReservations.get(i)::clashesWith)
                        .findFirst()
                        .ifPresent(clash -> {
                            throw new ResourceUnavailableException(clash.reservation().period());
                        }));
    }

    private void requireInStock(SupplyDemand demand, Set<ExpeditionId> ignoredOwners) {
        if (!demand.supply().hasStockFor(demand.total(), ignoredOwners)) {
            throw new InsufficientStockException(demand.supply().getId(), demand.total());
        }
    }

    private record PlannedReservation(ReusableResource resource, Reservation reservation) {

        boolean isFree(Set<ExpeditionId> ignoredOwners) {
            return resource.isAvailableDuring(reservation.period(), ignoredOwners);
        }

        boolean clashesWith(PlannedReservation other) {
            return resource.equals(other.resource) && reservation.overlapsWith(other.reservation.period());
        }

        void book() {
            resource.reserve(reservation);
        }
    }
}
