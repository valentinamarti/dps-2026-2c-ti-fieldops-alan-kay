package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.interfaces.resources.DepletableResource;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.Equipment;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.Resource;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * A resource together with the way an expedition assigns it. Whoever creates it knows what kind of resource it is,
 * so applying it later needs no question about its type.
 */
public final class ResourceAssignment {

    private final Resource resource;
    private final BiConsumer<Expedition, ItineraryItem> assignment;

    private ResourceAssignment(Resource resource, BiConsumer<Expedition, ItineraryItem> assignment) {
        this.resource = Objects.requireNonNull(resource, "resource is required");
        this.assignment = assignment;
    }

    public static ResourceAssignment of(Person person) {
        return new ResourceAssignment(person, (expedition, item) -> expedition.assignStaff(item, person));
    }

    public static ResourceAssignment of(Equipment equipment) {
        return new ResourceAssignment(equipment, (expedition, item) -> expedition.assignEquipment(item, equipment));
    }

    public static ResourceAssignment of(DepletableResource supply) {
        return new ResourceAssignment(supply, (expedition, item) -> expedition.assignSupply(item, supply));
    }

    public Resource resource() {
        return resource;
    }

    public void applyTo(Expedition expedition, ItineraryItem item) {
        assignment.accept(expedition, item);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ResourceAssignment that && resource.equals(that.resource);
    }

    @Override
    public int hashCode() {
        return resource.hashCode();
    }

    @Override
    public String toString() {
        return resource.getId().toString();
    }
}
