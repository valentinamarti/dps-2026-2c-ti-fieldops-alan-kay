package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.RegisterStaffRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.StaffResponse;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.commands.RegisterPersonCommand;
import ar.edu.itba.dps.fieldops.business.models.responses.PersonView;

public final class StaffMapper {
    private StaffMapper() {
    }

    public static RegisterPersonCommand toCommand(RegisterStaffRequest request) {
        return new RegisterPersonCommand(
                new ResourceId(request.id()),
                request.name(),
                Mappings.toCertifications(request.certifications()));
    }

    public static StaffResponse toResponse(PersonView person) {
        return new StaffResponse(
                person.id().value(),
                person.name(),
                Mappings.certificationNames(person.certifications()));
    }
}
