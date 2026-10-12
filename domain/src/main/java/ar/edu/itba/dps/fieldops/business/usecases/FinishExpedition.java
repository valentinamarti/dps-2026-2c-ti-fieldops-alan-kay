package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.services.ResourceReservations;

import java.util.Objects;

public class FinishExpedition {

    private final ResourceReservations reservations;
    private final ExpeditionRepository repository;

    public FinishExpedition(ResourceReservations reservations, ExpeditionRepository repository) {
        this.reservations = Objects.requireNonNull(reservations, "reservations are required");
        this.repository = Objects.requireNonNull(repository, "repository is required");
    }

    public void finish(ExpeditionId id) {
        finish(Lookups.expedition(repository, id));
    }

    public void finish(Expedition expedition) {
        expedition.finish();
        reservations.releaseFor(expedition);
    }
}
