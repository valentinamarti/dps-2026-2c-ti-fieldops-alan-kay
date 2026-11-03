package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Observation;
import ar.edu.itba.dps.fieldops.business.ports.in.TrackActivityPort;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;

import java.time.LocalDateTime;
import java.util.Objects;

public class TrackActivity implements TrackActivityPort {

    private final ExpeditionRepository repository;
    private final ResourceCatalog catalog;

    public TrackActivity(ExpeditionRepository repository, ResourceCatalog catalog) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
        this.catalog = Objects.requireNonNull(catalog, "catalog is required");
    }

    @Override
    public void start(ExpeditionId expeditionId, ActivityId activityId, LocalDateTime at) {
        final var expedition = Lookups.expedition(repository, expeditionId);
        expedition.startActivity(Lookups.item(expedition, activityId), at);
        repository.save(expedition);
    }

    @Override
    public void finish(ExpeditionId expeditionId, ActivityId activityId, LocalDateTime at, String result) {
        final var expedition = Lookups.expedition(repository, expeditionId);
        final var item = Lookups.item(expedition, activityId);
        expedition.finishActivity(item, at, result);
        repository.save(expedition);
        item.getAssignedSupplies().forEach(catalog::save);
    }

    @Override
    public void observe(ExpeditionId expeditionId, ActivityId activityId, Observation observation) {
        final var expedition = Lookups.expedition(repository, expeditionId);
        expedition.recordObservation(Lookups.item(expedition, activityId), observation);
        repository.save(expedition);
    }
}
