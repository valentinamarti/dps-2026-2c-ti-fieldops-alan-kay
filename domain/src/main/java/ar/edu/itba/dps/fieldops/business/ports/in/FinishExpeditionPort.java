package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;

public interface FinishExpeditionPort {
    void finish(ExpeditionId id);
}
