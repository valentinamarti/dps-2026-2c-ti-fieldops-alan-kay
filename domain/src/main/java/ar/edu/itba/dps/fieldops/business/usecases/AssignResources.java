package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;

import java.util.Objects;

public class AssignResources {

    private final ExpeditionRepository repository;
    private final ResourceCatalog catalog;

    public AssignResources(ExpeditionRepository repository, ResourceCatalog catalog) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
        this.catalog = Objects.requireNonNull(catalog, "catalog is required");
    }

    public void assignStaff(ExpeditionId expeditionId, ActivityId activityId, ResourceId personId) {
        final var expedition = Lookups.expedition(repository, expeditionId);
        expedition.assignStaff(Lookups.item(expedition, activityId), Lookups.staff(catalog, personId));
    }

    public void assignEquipment(ExpeditionId expeditionId, ActivityId activityId, ResourceId equipmentId) {
        final var expedition = Lookups.expedition(repository, expeditionId);
        expedition.assignEquipment(Lookups.item(expedition, activityId), Lookups.equipment(catalog, equipmentId));
    }

    public void assignSupply(ExpeditionId expeditionId, ActivityId activityId, ResourceId supplyId) {
        final var expedition = Lookups.expedition(repository, expeditionId);
        expedition.assignSupply(Lookups.item(expedition, activityId), Lookups.supply(catalog, supplyId));
    }
}
