package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.ports.in.FinishExpeditionPort;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.services.ResourceReservations;

import java.util.Objects;

public class FinishExpedition implements FinishExpeditionPort {

    private final ResourceReservations reservations;
    private final ExpeditionRepository repository;
    private final ResourceCatalog catalog;

    public FinishExpedition(ResourceReservations reservations, ExpeditionRepository repository, ResourceCatalog catalog) {
        this.reservations = Objects.requireNonNull(reservations, "reservations are required");
        this.repository = Objects.requireNonNull(repository, "repository is required");
        this.catalog = Objects.requireNonNull(catalog, "catalog is required");
    }

    @Override
    public void finish(ExpeditionId id) {
        finish(Lookups.expedition(repository, id));
    }

    void finish(Expedition expedition) {
        expedition.finish();
        reservations.releaseFor(expedition);
        repository.save(expedition);
        expedition.assignedResources().forEach(catalog::save);
    }
}
