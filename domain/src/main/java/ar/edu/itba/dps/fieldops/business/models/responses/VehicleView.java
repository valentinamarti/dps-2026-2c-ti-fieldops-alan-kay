package ar.edu.itba.dps.fieldops.business.models.responses;

import ar.edu.itba.dps.fieldops.business.models.resources.Certifications;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.resources.Vehicle;

public record VehicleView(ResourceId id, String name, ResourceCategory category, String licensePlate,
                          Certifications certifications) {

    public static VehicleView of(Vehicle vehicle) {
        return new VehicleView(vehicle.getId(), vehicle.getName(), vehicle.getCategory(), vehicle.getLicensePlate(),
                vehicle.getCertifications());
    }
}
