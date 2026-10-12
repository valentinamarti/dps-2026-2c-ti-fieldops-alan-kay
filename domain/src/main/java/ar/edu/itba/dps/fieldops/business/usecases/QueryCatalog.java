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

/**
 * Reads the catalog: the whole list of each kind and one entry by id, which fails with
 * {@link NotFoundException} when there is nothing registered under that id (D38).
 *
 * <p>It answers with the model classes and not with the {@code Equipment} and {@code DepletableResource}
 * interfaces, because what the API shows of a resource (its category, its license plate, its stock) is not
 * what those interfaces ask of it (D52). Vehicles and instruments are both equipment, so telling them apart
 * is selecting the ones of a type; nothing here asks a resource its type to decide what to do with it.
 */
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
