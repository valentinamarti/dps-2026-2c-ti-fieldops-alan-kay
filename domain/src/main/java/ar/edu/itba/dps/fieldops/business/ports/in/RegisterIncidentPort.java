package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.commands.RegisterIncidentCommand;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;

public interface RegisterIncidentPort {
    Incident register(RegisterIncidentCommand command);
}
