package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.commands.ScheduleActivityCommand;
import ar.edu.itba.dps.fieldops.business.models.responses.ScheduledActivityView;

public interface ScheduleActivityPort {
    ScheduledActivityView schedule(ScheduleActivityCommand command);
}
