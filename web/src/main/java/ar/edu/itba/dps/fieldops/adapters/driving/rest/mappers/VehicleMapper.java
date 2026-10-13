package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.RegisterVehicleRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.VehicleResponse;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.resources.Vehicle;
import ar.edu.itba.dps.fieldops.business.usecases.RegisterVehicleCommand;

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

    public static VehicleResponse toResponse(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId().value(),
                vehicle.getName(),
                vehicle.getCategory().name(),
                vehicle.getLicensePlate(),
                Mappings.certificationNames(vehicle.getCertifications()));
    }
}
