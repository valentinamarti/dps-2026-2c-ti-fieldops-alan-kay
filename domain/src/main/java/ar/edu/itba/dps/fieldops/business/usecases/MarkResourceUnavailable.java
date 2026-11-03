package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.interfaces.resources.ReusableResource;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.resources.Unavailability;
import ar.edu.itba.dps.fieldops.business.ports.in.MarkResourceUnavailablePort;

import java.util.Objects;

public class MarkResourceUnavailable implements MarkResourceUnavailablePort {
    private final ResourceCatalog catalog;

    public MarkResourceUnavailable(ResourceCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog is required");
    }

    @Override
    public void markStaffUnavailable(ResourceId id, Unavailability unavailability) {
        markUnavailable(Lookups.staff(catalog, id), unavailability);
    }

    @Override
    public void markVehicleUnavailable(ResourceId id, Unavailability unavailability) {
        markUnavailable(Lookups.vehicle(catalog, id), unavailability);
    }

    @Override
    public void markInstrumentUnavailable(ResourceId id, Unavailability unavailability) {
        markUnavailable(Lookups.instrument(catalog, id), unavailability);
    }

    private void markUnavailable(ReusableResource resource, Unavailability unavailability) {
        resource.markUnavailable(unavailability);
        catalog.save(resource);
    }
}
