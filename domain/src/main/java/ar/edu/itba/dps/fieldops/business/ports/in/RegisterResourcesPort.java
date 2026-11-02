package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.commands.RegisterInstrumentCommand;
import ar.edu.itba.dps.fieldops.business.models.commands.RegisterPersonCommand;
import ar.edu.itba.dps.fieldops.business.models.commands.RegisterSupplyCommand;
import ar.edu.itba.dps.fieldops.business.models.commands.RegisterVehicleCommand;
import ar.edu.itba.dps.fieldops.business.models.responses.InstrumentView;
import ar.edu.itba.dps.fieldops.business.models.responses.PersonView;
import ar.edu.itba.dps.fieldops.business.models.responses.SupplyView;
import ar.edu.itba.dps.fieldops.business.models.responses.VehicleView;

public interface RegisterResourcesPort {
    PersonView registerPerson(RegisterPersonCommand command);

    VehicleView registerVehicle(RegisterVehicleCommand command);

    InstrumentView registerInstrument(RegisterInstrumentCommand command);

    SupplyView registerSupply(RegisterSupplyCommand command);
}
