package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;

import java.util.Set;

/**
 * Everything needed to register a vehicle in the catalog. A vehicle is the only piece of equipment with a
 * license plate, which is why it has its own command and its own sub-resource (D52).
 */
public record RegisterVehicleCommand(ResourceId id, String name, ResourceCategory category, String licensePlate,
                                     Set<Certification> certifications) {
}
