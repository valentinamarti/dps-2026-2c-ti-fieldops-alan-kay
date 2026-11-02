package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.interfaces.itinerary.ItineraryNode;
import ar.edu.itba.dps.fieldops.business.models.commands.ScheduleBlockCommand;

public interface ScheduleBlockPort {
    ItineraryNode schedule(ScheduleBlockCommand command);
}
