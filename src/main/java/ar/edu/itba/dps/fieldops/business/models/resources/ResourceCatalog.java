package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.exceptions.DuplicateResourceException;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.DepletableResource;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.Equipment;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.Resource;
import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ResourceCatalog {

    private final Set<String> registeredIds = new HashSet<>();
    private final List<Person> staff = new ArrayList<>();
    private final List<Equipment> equipment = new ArrayList<>();
    private final List<DepletableResource> supplies = new ArrayList<>();

    public void addStaff(Person person) {
        register(person);
        staff.add(person);
    }

    public void addEquipment(Equipment item) {
        register(item);
        equipment.add(item);
    }

    public void addSupply(DepletableResource supply) {
        register(supply);
        supplies.add(supply);
    }

    public List<Person> availableStaffFor(StaffRequirement requirement, TimePeriod period) {
        return staff.stream()
                .filter(requirement::qualifies)
                .filter(person -> person.isAvailableDuring(period))
                .toList();
    }

    public List<Equipment> availableEquipmentFor(ReusableRequirement requirement, TimePeriod period) {
        return equipment.stream()
                .filter(requirement::accepts)
                .filter(item -> item.isAvailableDuring(period))
                .toList();
    }

    public List<DepletableResource> suppliesCovering(DepletableRequirement requirement) {
        return supplies.stream()
                .filter(requirement::isCoveredBy)
                .toList();
    }

    private void register(Resource resource) {
        Objects.requireNonNull(resource, "resource is required");
        if (!registeredIds.add(resource.getId())) {
            throw new DuplicateResourceException(resource.getId());
        }
    }
}
