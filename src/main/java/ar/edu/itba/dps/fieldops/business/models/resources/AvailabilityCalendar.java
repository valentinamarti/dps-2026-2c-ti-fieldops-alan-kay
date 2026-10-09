package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.exceptions.ResourceUnavailableException;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AvailabilityCalendar {

    private final List<Reservation> reservations = new ArrayList<>();

    public boolean isFreeDuring(TimePeriod period) {
        return reservations.stream().noneMatch(reservation -> reservation.overlapsWith(period));
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
}
