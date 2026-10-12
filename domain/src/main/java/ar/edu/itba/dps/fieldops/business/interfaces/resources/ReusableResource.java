package ar.edu.itba.dps.fieldops.business.interfaces.resources;

import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.resources.Reservation;
import ar.edu.itba.dps.fieldops.business.models.resources.Unavailability;

import java.util.Set;

public interface ReusableResource extends Resource {

    boolean isAvailableDuring(TimePeriod period);

    boolean isAvailableDuring(TimePeriod period, Set<ExpeditionId> ignoredOwners);

    void reserve(Reservation reservation);

    void releaseAll(ExpeditionId owner);

    void markUnavailable(Unavailability unavailability);
}
