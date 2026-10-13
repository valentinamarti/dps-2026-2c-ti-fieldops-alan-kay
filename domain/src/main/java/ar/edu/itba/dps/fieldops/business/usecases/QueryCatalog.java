package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.NotFoundException;
import ar.edu.itba.dps.fieldops.business.models.resources.Depletable;
import ar.edu.itba.dps.fieldops.business.models.resources.Instrument;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.resources.Vehicle;

import java.util.List;
import java.util.Objects;

public class QueryCatalog {
    private final ResourceCatalog catalog;

    public QueryCatalog(ResourceCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog is required");
    }

    public List<Person> staff() {
        return catalog.allStaff();
    }

    public Person person(ResourceId id) {
        return Lookups.staff(catalog, id);
    }

    public List<Vehicle> vehicles() {
        return onlyOf(catalog.allEquipment(), Vehicle.class);
    }

    public Vehicle vehicle(ResourceId id) {
        return Lookups.vehicle(catalog, id);
    }

    public List<Instrument> instruments() {
        return onlyOf(catalog.allEquipment(), Instrument.class);
    }

    public Instrument instrument(ResourceId id) {
        return Lookups.instrument(catalog, id);
    }

    public List<Depletable> supplies() {
        return onlyOf(catalog.allSupplies(), Depletable.class);
    }

    public Depletable supply(ResourceId id) {
        return Lookups.depletable(catalog, id);
    }

    private static <T> List<T> onlyOf(List<?> resources, Class<T> kind) {
        return resources.stream().filter(kind::isInstance).map(kind::cast).toList();
    }
}
