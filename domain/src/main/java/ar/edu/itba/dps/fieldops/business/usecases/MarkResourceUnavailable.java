package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.resources.Unavailability;

import java.util.Objects;

/**
 * Records a period in which a resource of the catalog cannot be used, so the assignments and the validations
 * stop counting on it. Only the reusable resources have a calendar: a depletable supply has stock, not
 * availability, so there is no method for one.
 *
 * <p>There is a method per kind, like {@link AssignResources}: the id that arrives has to be the one of the
 * kind that is being marked, and an id of another kind fails with "there is no vehicle with id ...".
 */
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
