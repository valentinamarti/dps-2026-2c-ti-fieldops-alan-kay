package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.validation.ApprovalResult;

public interface ValidateExpeditionPort {
    ApprovalResult validate(ExpeditionId id);
}
