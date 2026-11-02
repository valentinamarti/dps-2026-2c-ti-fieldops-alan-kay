package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.commands.RegisterZoneCommand;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;

public interface RegisterZonePort {
    Zone register(RegisterZoneCommand command);
}
