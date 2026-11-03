package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.estimation.ExpeditionEstimate;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;

public interface EstimateExpeditionPort {
    ExpeditionEstimate estimate(ExpeditionId id);
}
