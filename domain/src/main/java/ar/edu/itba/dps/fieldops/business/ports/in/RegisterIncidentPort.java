package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.commands.RegisterIncidentCommand;
import ar.edu.itba.dps.fieldops.business.models.responses.ReplanProposalView;

public interface RegisterIncidentPort {
    ReplanProposalView register(RegisterIncidentCommand command);
}
