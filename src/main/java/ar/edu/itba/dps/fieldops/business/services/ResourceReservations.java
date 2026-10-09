package ar.edu.itba.dps.fieldops.business.services;

import ar.edu.itba.dps.fieldops.business.exceptions.ResourceUnavailableException;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.ReusableResource;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.resources.Reservation;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class ResourceReservations {

    public void requireReservable(Expedition expedition) {
        requireReservable(commitmentsOf(expedition));
    }

    public void reserveFor(Expedition expedition) {
        final var commitments = commitmentsOf(expedition);
        requireReservable(commitments);
        commitments.forEach(Commitment::book);
    }

    private void requireReservable(List<Commitment> commitments) {
        commitments.forEach(this::requireFree);
        requireNoClashesBetween(commitments);
    }

    private List<Commitment> commitmentsOf(Expedition expedition) {
        return expedition.getItinerary().getItems().stream()
                .flatMap(item -> commitmentsOf(expedition, item))
                .toList();
    }

    private Stream<Commitment> commitmentsOf(Expedition expedition, ItineraryItem item) {
        final var reservation = new Reservation(expedition.getId(), item.getScheduledPeriod());
        return item.getAssignedReusableResources().stream().map(resource -> new Commitment(resource, reservation));
    }

    private void requireFree(Commitment commitment) {
        if (!commitment.isFree()) {
            throw new ResourceUnavailableException(commitment.reservation().period());
        }
    }


    private void requireNoClashesBetween(List<Commitment> commitments) {
        IntStream.range(0, commitments.size()).forEach(i ->
                commitments.subList(i + 1, commitments.size()).stream()
                        .filter(commitments.get(i)::clashesWith)
                        .findFirst()
                        .ifPresent(clash -> {
                            throw new ResourceUnavailableException(clash.reservation().period());
                        }));
    }

    private record Commitment(ReusableResource resource, Reservation reservation) {

        boolean isFree() {
            return resource.isAvailableDuring(reservation.period());
        }

        boolean clashesWith(Commitment other) {
            return resource.equals(other.resource) && reservation.overlapsWith(other.reservation.period());
        }

        void book() {
            resource.reserve(reservation);
        }
    }
}
