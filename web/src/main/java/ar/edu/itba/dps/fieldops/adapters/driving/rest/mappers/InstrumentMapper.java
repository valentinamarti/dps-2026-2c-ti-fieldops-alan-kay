package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.InstrumentResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.RegisterInstrumentRequest;
import ar.edu.itba.dps.fieldops.business.models.resources.Instrument;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.usecases.RegisterInstrumentCommand;

public final class InstrumentMapper {
    private InstrumentMapper() {
    }

    public static RegisterInstrumentCommand toCommand(RegisterInstrumentRequest request) {
        return new RegisterInstrumentCommand(
                new ResourceId(request.id()),
                request.name(),
                new ResourceCategory(request.category()),
                Mappings.toCertifications(request.certifications()));
    }

    public static InstrumentResponse toResponse(Instrument instrument) {
        return new InstrumentResponse(
                instrument.getId().value(),
                instrument.getName(),
                instrument.getCategory().name(),
                Mappings.certificationNames(instrument.getCertifications()));
    }
}
