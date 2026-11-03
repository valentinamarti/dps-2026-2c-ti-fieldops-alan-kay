package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.InstrumentResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.RegisterInstrumentRequest;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.commands.RegisterInstrumentCommand;
import ar.edu.itba.dps.fieldops.business.models.responses.InstrumentView;

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

    public static InstrumentResponse toResponse(InstrumentView instrument) {
        return new InstrumentResponse(
                instrument.id().value(),
                instrument.name(),
                instrument.category().name(),
                Mappings.certificationNames(instrument.certifications()));
    }
}
