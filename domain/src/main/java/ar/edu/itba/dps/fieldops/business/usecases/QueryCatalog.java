package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.NotFoundException;
import ar.edu.itba.dps.fieldops.business.models.resources.Depletable;
import ar.edu.itba.dps.fieldops.business.models.resources.Instrument;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.resources.Vehicle;
import ar.edu.itba.dps.fieldops.business.ports.in.QueryCatalogPort;

import java.util.List;
import java.util.Objects;

public class QueryCatalog implements QueryCatalogPort {
    private final ResourceCatalog catalog;

    public QueryCatalog(ResourceCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog is required");
    }

    @Override
    public List<Person> staff() {
        return catalog.allStaff();
    }

    @Override
    public Person person(ResourceId id) {
        return Lookups.staff(catalog, id);
    }

    @Override
    public List<Vehicle> vehicles() {
        return onlyOf(catalog.allEquipment(), Vehicle.class);
    }

    @Override
    public Vehicle vehicle(ResourceId id) {
        return Lookups.vehicle(catalog, id);
    }

    @Override
    public List<Instrument> instruments() {
        return onlyOf(catalog.allEquipment(), Instrument.class);
    }

    @Override
    public Instrument instrument(ResourceId id) {
        return Lookups.instrument(catalog, id);
    }

    @Override
    public List<Depletable> supplies() {
        return onlyOf(catalog.allSupplies(), Depletable.class);
    }

    @Override
    public Depletable supply(ResourceId id) {
        return Lookups.depletable(catalog, id);
    }

    private static <T> List<T> onlyOf(List<?> resources, Class<T> kind) {
        return resources.stream().filter(kind::isInstance).map(kind::cast).toList();
    }
}
