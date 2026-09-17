package ar.edu.itba.dps.fieldops.business.interfaces.validation;

import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.validation.ApprovalResult;

public interface ExpeditionValidator {

    ApprovalResult validate(Expedition expedition);
}