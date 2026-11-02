package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.commands.CreateExpeditionCommand;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;

public interface CreateExpeditionPort {
    Expedition create(CreateExpeditionCommand command);
}
