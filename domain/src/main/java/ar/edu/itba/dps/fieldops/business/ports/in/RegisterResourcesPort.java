package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.commands.RegisterInstrumentCommand;
import ar.edu.itba.dps.fieldops.business.models.commands.RegisterPersonCommand;
import ar.edu.itba.dps.fieldops.business.models.commands.RegisterSupplyCommand;
import ar.edu.itba.dps.fieldops.business.models.commands.RegisterVehicleCommand;
import ar.edu.itba.dps.fieldops.business.models.resources.Depletable;
import ar.edu.itba.dps.fieldops.business.models.resources.Instrument;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.Vehicle;

public interface RegisterResourcesPort {
    Person registerPerson(RegisterPersonCommand command);

    Vehicle registerVehicle(RegisterVehicleCommand command);

    Instrument registerInstrument(RegisterInstrumentCommand command);

    Depletable registerSupply(RegisterSupplyCommand command);
}
