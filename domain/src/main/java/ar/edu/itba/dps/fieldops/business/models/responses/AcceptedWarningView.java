package ar.edu.itba.dps.fieldops.business.models.responses;

import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.validation.AcceptedWarning;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;

public record AcceptedWarningView(ValidationResult warning, ResourceId responsibleId, String justification) {

    static AcceptedWarningView of(AcceptedWarning accepted) {
        return new AcceptedWarningView(accepted.warning(), accepted.responsible().getId(), accepted.justification());
    }
}
