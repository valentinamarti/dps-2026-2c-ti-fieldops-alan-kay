package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.commands.ScheduleActivityCommand;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;

public interface ScheduleActivityPort {
    ItineraryItem schedule(ScheduleActivityCommand command);
}
