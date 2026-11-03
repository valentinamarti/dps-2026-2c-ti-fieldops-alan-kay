package ar.edu.itba.dps.fieldops.business.providers;

import ar.edu.itba.dps.fieldops.business.interfaces.resources.DepletableResource;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.Equipment;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.Resource;
import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;

import java.util.List;
import java.util.Optional;

public interface ResourceCatalog {

    void addStaff(Person person);

    void addEquipment(Equipment item);

    void addSupply(DepletableResource supply);

    void save(Resource resource);

    Optional<Person> findStaff(ResourceId id);

    Optional<Equipment> findEquipment(ResourceId id);

    Optional<DepletableResource> findSupply(ResourceId id);

    List<Person> allStaff();

    List<Equipment> allEquipment();

    List<DepletableResource> allSupplies();

    default List<Person> availableStaffFor(StaffRequirement requirement, TimePeriod period) {
        return allStaff().stream()
                .filter(requirement::qualifies)
                .filter(person -> person.isAvailableDuring(period))
                .toList();
    }

    default List<Equipment> availableEquipmentFor(ReusableRequirement requirement, TimePeriod period) {
        return allEquipment().stream()
                .filter(requirement::accepts)
                .filter(item -> item.isAvailableDuring(period))
                .toList();
    }

    default List<DepletableResource> suppliesCovering(DepletableRequirement requirement) {
        return allSupplies().stream()
                .filter(requirement::isCoveredBy)
                .toList();
    }
}
