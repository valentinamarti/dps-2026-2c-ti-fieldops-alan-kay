package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.resources.Unavailability;

import java.util.Objects;

public class MarkResourceUnavailable {
    private final ResourceCatalog catalog;

    public MarkResourceUnavailable(ResourceCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog is required");
    }

    public void markStaffUnavailable(ResourceId id, Unavailability unavailability) {
        Lookups.staff(catalog, id).markUnavailable(unavailability);
    }

    public void markVehicleUnavailable(ResourceId id, Unavailability unavailability) {
        Lookups.vehicle(catalog, id).markUnavailable(unavailability);
    }

    public void markInstrumentUnavailable(ResourceId id, Unavailability unavailability) {
        Lookups.instrument(catalog, id).markUnavailable(unavailability);
    }
}
