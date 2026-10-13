package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.DuplicateResourceException;
import ar.edu.itba.dps.fieldops.business.models.resources.Depletable;
import ar.edu.itba.dps.fieldops.business.models.resources.Instrument;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.resources.Vehicle;

import java.util.Objects;

public class RegisterResources {
    private final ResourceCatalog catalog;

    public RegisterResources(ResourceCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog is required");
    }

    public Person registerPerson(RegisterPersonCommand command) {
        final var person = new Person(command.id(), command.name(), command.certifications());
        catalog.addStaff(person);
        return person;
    }

    public Vehicle registerVehicle(RegisterVehicleCommand command) {
        final var vehicle = new Vehicle(command.id(), command.name(), command.category(), command.licensePlate(),
                command.certifications());
        catalog.addEquipment(vehicle);
        return vehicle;
    }

    public Instrument registerInstrument(RegisterInstrumentCommand command) {
        final var instrument = new Instrument(command.id(), command.name(), command.category(), command.certifications());
        catalog.addEquipment(instrument);
        return instrument;
    }

    public Depletable registerSupply(RegisterSupplyCommand command) {
        final var supply = new Depletable(command.id(), command.name(), command.category(), command.initialStock());
        catalog.addSupply(supply);
        return supply;
    }
}
