package ar.edu.itba.dps.fieldops.business.interfaces.resources;

import ar.edu.itba.dps.fieldops.business.models.resources.Depletable;
import ar.edu.itba.dps.fieldops.business.models.resources.Instrument;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.Vehicle;

public interface ResourceVisitor<T> {
    T person(Person person);

    T vehicle(Vehicle vehicle);

    T instrument(Instrument instrument);

    T supply(Depletable supply);
}
