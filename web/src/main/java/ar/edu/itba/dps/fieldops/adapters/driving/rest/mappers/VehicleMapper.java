package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.RegisterVehicleRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.VehicleResponse;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.commands.RegisterVehicleCommand;
import ar.edu.itba.dps.fieldops.business.models.responses.VehicleView;

public final class VehicleMapper {
    private VehicleMapper() {
    }

    public static RegisterVehicleCommand toCommand(RegisterVehicleRequest request) {
        return new RegisterVehicleCommand(
                new ResourceId(request.id()),
                request.name(),
                new ResourceCategory(request.category()),
                request.licensePlate(),
                Mappings.toCertifications(request.certifications()));
    }

    public static VehicleResponse toResponse(VehicleView vehicle) {
        return new VehicleResponse(
                vehicle.id().value(),
                vehicle.name(),
                vehicle.category().name(),
                vehicle.licensePlate(),
                Mappings.certificationNames(vehicle.certifications()));
    }
}
