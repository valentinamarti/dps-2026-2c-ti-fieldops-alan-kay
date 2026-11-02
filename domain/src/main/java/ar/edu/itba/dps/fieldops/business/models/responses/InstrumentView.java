package ar.edu.itba.dps.fieldops.business.models.responses;

import ar.edu.itba.dps.fieldops.business.models.resources.Certifications;
import ar.edu.itba.dps.fieldops.business.models.resources.Instrument;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;

public record InstrumentView(ResourceId id, String name, ResourceCategory category, Certifications certifications) {

    public static InstrumentView of(Instrument instrument) {
        return new InstrumentView(instrument.getId(), instrument.getName(), instrument.getCategory(),
                instrument.getCertifications());
    }
}
