package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.exceptions.ResourceUnavailableException;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;

import java.util.ArrayList;
import java.util.List;

public class AvailabilityCalendar {

    private final List<TimePeriod> bookedPeriods = new ArrayList<>();

    public boolean isFreeDuring(TimePeriod period) {
        return bookedPeriods.stream().noneMatch(period::overlapsWith);
    }

    public void book(TimePeriod period) {
        if (!isFreeDuring(period)) {
            throw new ResourceUnavailableException(period);
        }
        bookedPeriods.add(period);
    }
}
