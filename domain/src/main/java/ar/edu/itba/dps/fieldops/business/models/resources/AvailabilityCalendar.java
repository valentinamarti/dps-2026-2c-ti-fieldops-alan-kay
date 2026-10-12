package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.exceptions.ResourceUnavailableException;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class AvailabilityCalendar {

    private final List<Reservation> reservations = new ArrayList<>();
    private final List<Unavailability> unavailabilities = new ArrayList<>();

    public boolean isFreeDuring(TimePeriod period) {
        return isFreeDuring(period, Set.of());
    }

    public boolean isFreeDuring(TimePeriod period, Set<ExpeditionId> ignoredOwners) {
        return isNotReservedDuring(period, ignoredOwners) && isNotUnavailableDuring(period);
    }

    public void book(Reservation reservation) {
        Objects.requireNonNull(reservation, "reservation is required");
        if (!isFreeDuring(reservation.period())) {
            throw new ResourceUnavailableException(reservation.period());
        }
        reservations.add(reservation);
    }

    public void releaseAll(ExpeditionId owner) {
        Objects.requireNonNull(owner, "owner is required");
        reservations.removeIf(reservation -> reservation.isOwnedBy(owner));
    }

    public void markUnavailable(Unavailability unavailability) {
        unavailabilities.add(Objects.requireNonNull(unavailability, "unavailability is required"));
    }

    private boolean isNotReservedDuring(TimePeriod period, Set<ExpeditionId> ignoredOwners) {
        return reservations.stream()
                .filter(reservation -> !ignoredOwners.contains(reservation.owner()))
                .noneMatch(reservation -> reservation.overlapsWith(period));
    }

    private boolean isNotUnavailableDuring(TimePeriod period) {
        return unavailabilities.stream().noneMatch(unavailability -> unavailability.overlapsWith(period));
    }
}
