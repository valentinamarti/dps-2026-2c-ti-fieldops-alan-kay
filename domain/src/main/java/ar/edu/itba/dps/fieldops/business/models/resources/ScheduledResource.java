package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.interfaces.resources.ReusableResource;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;

import java.util.Set;

public abstract class ScheduledResource implements ReusableResource {

    private final AvailabilityCalendar calendar = new AvailabilityCalendar();

    @Override
    public boolean isAvailableDuring(TimePeriod period) {
        return calendar.isFreeDuring(period);
    }

    @Override
    public boolean isAvailableDuring(TimePeriod period, Set<ExpeditionId> ignoredOwners) {
        return calendar.isFreeDuring(period, ignoredOwners);
    }

    @Override
    public void reserve(Reservation reservation) {
        calendar.book(reservation);
    }

    @Override
    public void releaseAll(ExpeditionId owner) {
        calendar.releaseAll(owner);
    }

    @Override
    public void markUnavailable(Unavailability unavailability) {
        calendar.markUnavailable(unavailability);
    }
}
