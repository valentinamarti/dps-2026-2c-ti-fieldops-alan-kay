package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.commands.CreateExpeditionCommand;
import ar.edu.itba.dps.fieldops.business.models.responses.ExpeditionView;

public interface CreateExpeditionPort {
    ExpeditionView create(CreateExpeditionCommand command);
}
