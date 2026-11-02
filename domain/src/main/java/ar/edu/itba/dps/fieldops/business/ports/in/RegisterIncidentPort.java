package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.commands.RegisterIncidentCommand;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposal;

public interface RegisterIncidentPort {
    ReplanProposal register(RegisterIncidentCommand command);
}
