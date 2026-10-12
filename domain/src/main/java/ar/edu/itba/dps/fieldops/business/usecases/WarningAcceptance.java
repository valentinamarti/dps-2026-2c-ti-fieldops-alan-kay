package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.validation.RuleId;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationSubject;

/**
 * A responsible accepting the warning that a rule raised about a subject, with the reason why.
 */
public record WarningAcceptance(RuleId rule, ValidationSubject subject, ResourceId responsibleId, String justification) {
}
