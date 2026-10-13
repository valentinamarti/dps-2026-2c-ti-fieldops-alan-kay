package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.validation.RuleId;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationSubject;

public record WarningAcceptance(RuleId rule, ValidationSubject subject, ResourceId responsibleId, String justification) {
}
