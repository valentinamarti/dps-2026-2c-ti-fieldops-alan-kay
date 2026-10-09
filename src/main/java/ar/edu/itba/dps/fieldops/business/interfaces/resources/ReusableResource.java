package ar.edu.itba.dps.fieldops.business.interfaces.resources;

import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.resources.Reservation;
import ar.edu.itba.dps.fieldops.business.models.resources.Unavailability;

public interface ReusableResource extends Resource {

    boolean isAvailableDuring(TimePeriod period);

    void reserve(Reservation reservation);

    void releaseAll(ExpeditionId owner);

    void markUnavailable(Unavailability unavailability);
}
