package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.resources.Unavailability;

public interface MarkResourceUnavailablePort {
    void markStaffUnavailable(ResourceId id, Unavailability unavailability);

    void markVehicleUnavailable(ResourceId id, Unavailability unavailability);

    void markInstrumentUnavailable(ResourceId id, Unavailability unavailability);
}
