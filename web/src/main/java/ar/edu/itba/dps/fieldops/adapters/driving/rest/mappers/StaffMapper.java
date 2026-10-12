package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.RegisterStaffRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.StaffResponse;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.usecases.RegisterPersonCommand;

/**
 * Translates between the staff DTOs and the domain, in both directions (D49).
 */
public final class StaffMapper {

    private StaffMapper() {
    }

    public static RegisterPersonCommand toCommand(RegisterStaffRequest request) {
        return new RegisterPersonCommand(
                new ResourceId(request.id()),
                request.name(),
                Mappings.toCertifications(request.certifications()));
    }

    public static StaffResponse toResponse(Person person) {
        return new StaffResponse(
                person.getId().value(),
                person.getName(),
                Mappings.certificationNames(person.getCertifications()));
    }
}
