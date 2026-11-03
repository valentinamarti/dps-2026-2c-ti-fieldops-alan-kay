package ar.edu.itba.dps.fieldops.business.fixtures;

import ar.edu.itba.dps.fieldops.business.exceptions.DuplicateResourceException;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.DepletableResource;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.Equipment;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.Resource;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class FakeResourceCatalog implements ResourceCatalog {

    private final Set<ResourceId> registeredIds = new HashSet<>();
    private final List<Person> staff = new ArrayList<>();
    private final List<Equipment> equipment = new ArrayList<>();
    private final List<DepletableResource> supplies = new ArrayList<>();

    @Override
    public void addStaff(Person person) {
        register(person);
        staff.add(person);
    }

    @Override
    public void addEquipment(Equipment item) {
        register(item);
        equipment.add(item);
    }

    @Override
    public void addSupply(DepletableResource supply) {
        register(supply);
        supplies.add(supply);
    }

    @Override
    public void save(Resource resource) {
        Objects.requireNonNull(resource, "resource is required");
    }

    @Override
    public Optional<Person> findStaff(ResourceId id) {
        return staff.stream().filter(person -> person.getId().equals(id)).findFirst();
    }

    @Override
    public Optional<Equipment> findEquipment(ResourceId id) {
        return equipment.stream().filter(item -> item.getId().equals(id)).findFirst();
    }

    @Override
    public Optional<DepletableResource> findSupply(ResourceId id) {
        return supplies.stream().filter(supply -> supply.getId().equals(id)).findFirst();
    }

    @Override
    public List<Person> allStaff() {
        return List.copyOf(staff);
    }

    @Override
    public List<Equipment> allEquipment() {
        return List.copyOf(equipment);
    }

    @Override
    public List<DepletableResource> allSupplies() {
        return List.copyOf(supplies);
    }

    private void register(Resource resource) {
        Objects.requireNonNull(resource, "resource is required");
        if (!registeredIds.add(resource.getId())) {
            throw new DuplicateResourceException(resource.getId());
        }
    }
}
