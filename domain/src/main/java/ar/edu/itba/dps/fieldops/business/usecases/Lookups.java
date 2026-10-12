package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.NotFoundException;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.DepletableResource;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.Equipment;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.resources.Depletable;
import ar.edu.itba.dps.fieldops.business.models.resources.Instrument;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposal;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalId;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.resources.Vehicle;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ReplanProposalRepository;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;

import java.util.Optional;

/**
 * Turns the ids a use case receives into the objects it works with, or fails with {@link NotFoundException}.
 */
final class Lookups {

    private Lookups() {
    }

    static Expedition expedition(ExpeditionRepository repository, ExpeditionId id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("expedition", id));
    }

    static ReplanProposal proposal(ReplanProposalRepository repository, ReplanProposalId id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("replan proposal", id));
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

    static Zone zone(ZoneRepository zones, String id) {
        return zones.findById(id).orElseThrow(() -> new NotFoundException("zone", id));
    }

    static Vehicle vehicle(ResourceCatalog catalog, ResourceId id) {
        return ofKind(catalog.findEquipment(id), Vehicle.class, "vehicle", id);
    }

    static Instrument instrument(ResourceCatalog catalog, ResourceId id) {
        return ofKind(catalog.findEquipment(id), Instrument.class, "instrument", id);
    }

    static Depletable depletable(ResourceCatalog catalog, ResourceId id) {
        return ofKind(catalog.findSupply(id), Depletable.class, "supply", id);
    }

    /**
     * The catalog keeps every piece of equipment together, so an id that belongs to another kind is as good as
     * an id that nobody registered: both answer that there is no vehicle (or no instrument) with that id.
     */
    private static <T> T ofKind(Optional<?> found, Class<T> kind, String name, ResourceId id) {
        return found.filter(kind::isInstance).map(kind::cast).orElseThrow(() -> new NotFoundException(name, id));
    }
}
