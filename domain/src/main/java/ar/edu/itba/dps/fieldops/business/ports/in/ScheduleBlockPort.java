package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.commands.ScheduleBlockCommand;
import ar.edu.itba.dps.fieldops.business.models.responses.ItineraryNodeView;

public interface ScheduleBlockPort {
    ItineraryNodeView schedule(ScheduleBlockCommand command);
}
