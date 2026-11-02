package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.commands.RegisterInstrumentCommand;
import ar.edu.itba.dps.fieldops.business.models.commands.RegisterPersonCommand;
import ar.edu.itba.dps.fieldops.business.models.commands.RegisterSupplyCommand;
import ar.edu.itba.dps.fieldops.business.models.commands.RegisterVehicleCommand;
import ar.edu.itba.dps.fieldops.business.models.resources.Depletable;
import ar.edu.itba.dps.fieldops.business.models.resources.Instrument;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.resources.Vehicle;
import ar.edu.itba.dps.fieldops.business.models.responses.InstrumentView;
import ar.edu.itba.dps.fieldops.business.models.responses.PersonView;
import ar.edu.itba.dps.fieldops.business.models.responses.SupplyView;
import ar.edu.itba.dps.fieldops.business.models.responses.VehicleView;
import ar.edu.itba.dps.fieldops.business.ports.in.RegisterResourcesPort;

import java.util.Objects;

public class RegisterResources implements RegisterResourcesPort {
    private final ResourceCatalog catalog;

    public RegisterResources(ResourceCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog is required");
    }

    @Override
    public PersonView registerPerson(RegisterPersonCommand command) {
        final var person = new Person(command.id(), command.name(), command.certifications());
        catalog.addStaff(person);
        return PersonView.of(person);
    }

    @Override
    public VehicleView registerVehicle(RegisterVehicleCommand command) {
        final var vehicle = new Vehicle(command.id(), command.name(), command.category(), command.licensePlate(),
                command.certifications());
        catalog.addEquipment(vehicle);
        return VehicleView.of(vehicle);
    }

    @Override
    public InstrumentView registerInstrument(RegisterInstrumentCommand command) {
        final var instrument = new Instrument(command.id(), command.name(), command.category(), command.certifications());
        catalog.addEquipment(instrument);
        return InstrumentView.of(instrument);
    }

    @Override
    public SupplyView registerSupply(RegisterSupplyCommand command) {
        final var supply = new Depletable(command.id(), command.name(), command.category(), command.initialStock());
        catalog.addSupply(supply);
        return SupplyView.of(supply);
    }
}
