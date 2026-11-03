package ar.edu.itba.dps.fieldops.business.models.resources;

import java.util.List;

public record CalendarSnapshot(List<Reservation> reservations, List<Unavailability> unavailabilities) {

    public CalendarSnapshot {
        reservations = List.copyOf(reservations);
        unavailabilities = List.copyOf(unavailabilities);
    }
}
