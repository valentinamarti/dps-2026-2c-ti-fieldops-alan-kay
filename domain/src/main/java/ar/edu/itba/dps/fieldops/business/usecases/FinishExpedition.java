package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.services.ResourceReservations;

import java.util.Objects;

public class FinishExpedition {

    private final ResourceReservations reservations;

    public FinishExpedition(ResourceReservations reservations) {
        this.reservations = Objects.requireNonNull(reservations, "reservations are required");
    }

    public void finish(Expedition expedition) {
        expedition.finish();
        reservations.releaseFor(expedition);
    }
}
