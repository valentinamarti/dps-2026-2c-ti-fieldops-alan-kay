package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.commands.RegisterZoneCommand;
import ar.edu.itba.dps.fieldops.business.models.responses.ZoneView;

public interface RegisterZonePort {
    ZoneView register(RegisterZoneCommand command);
}
