package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.NotFoundException;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.DepletableResource;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.Equipment;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;

/**
 * Turns the ids a use case receives into the objects it works with, or fails with {@link NotFoundException}.
 */
final class Lookups {

    private Lookups() {
    }

    static Expedition expedition(ExpeditionRepository repository, ExpeditionId id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("expedition", id));
    }

    static ItineraryItem item(Expedition expedition, ActivityId activityId) {
        return expedition.getItinerary().itemFor(activityId)
                .orElseThrow(() -> new NotFoundException("scheduled activity", activityId));
    }

    static Person staff(ResourceCatalog catalog, ResourceId id) {
        return catalog.findStaff(id).orElseThrow(() -> new NotFoundException("person", id));
    }

    static Equipment equipment(ResourceCatalog catalog, ResourceId id) {
        return catalog.findEquipment(id).orElseThrow(() -> new NotFoundException("equipment", id));
    }

    static DepletableResource supply(ResourceCatalog catalog, ResourceId id) {
        return catalog.findSupply(id).orElseThrow(() -> new NotFoundException("supply", id));
    }
}
