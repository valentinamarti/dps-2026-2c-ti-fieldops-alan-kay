package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.interfaces.validation.ExpeditionValidator;
import ar.edu.itba.dps.fieldops.business.interfaces.validation.ValidationRule;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.validation.ApprovalResult;

import java.util.List;

public class ValidationOrchestrator implements ExpeditionValidator {

    private final List<ValidationRule> rules;

    public ValidationOrchestrator(List<ValidationRule> rules) {
        this.rules = DomainArguments.requireList(rules, "rules");
    }

    @Override
    public ApprovalResult validate(Expedition expedition) {
        return new ApprovalResult(rules.stream()
                .flatMap(rule -> rule.validate(expedition).stream())
                .toList());
    }
}