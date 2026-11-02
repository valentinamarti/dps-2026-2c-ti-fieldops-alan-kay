package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.responses.InstrumentView;
import ar.edu.itba.dps.fieldops.business.models.responses.PersonView;
import ar.edu.itba.dps.fieldops.business.models.responses.SupplyView;
import ar.edu.itba.dps.fieldops.business.models.responses.VehicleView;

import java.util.List;

public interface QueryCatalogPort {
    List<PersonView> staff();

    PersonView person(ResourceId id);

    List<VehicleView> vehicles();

    VehicleView vehicle(ResourceId id);

    List<InstrumentView> instruments();

    InstrumentView instrument(ResourceId id);

    List<SupplyView> supplies();

    SupplyView supply(ResourceId id);
}
