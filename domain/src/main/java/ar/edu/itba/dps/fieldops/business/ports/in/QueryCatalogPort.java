package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.resources.Depletable;
import ar.edu.itba.dps.fieldops.business.models.resources.Instrument;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.resources.Vehicle;

import java.util.List;

public interface QueryCatalogPort {
    List<Person> staff();

    Person person(ResourceId id);

    List<Vehicle> vehicles();

    Vehicle vehicle(ResourceId id);

    List<Instrument> instruments();

    Instrument instrument(ResourceId id);

    List<Depletable> supplies();

    Depletable supply(ResourceId id);
}
