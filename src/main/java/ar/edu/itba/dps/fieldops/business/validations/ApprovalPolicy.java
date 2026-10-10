package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.interfaces.validation.ExpeditionValidator;
import ar.edu.itba.dps.fieldops.business.interfaces.validation.ValidationRule;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.validation.ApprovalResult;

import java.util.List;
import java.util.stream.Stream;

public final class ApprovalPolicy implements ExpeditionValidator {

    private static final List<ValidationRule> MANDATORY_RULES = List.of(
            new ResourceOverlapValidation(),
            new MissingResourcesValidation(),
            new InsufficientSuppliesValidation(),
            new MissingCertificationsValidation(),
            new MissingPermitsValidation(),
            new CapacityExceededValidation(),
            new RiskToleranceValidation());

    private final List<ValidationRule> rules;

    public ApprovalPolicy(List<ValidationRule> additionalRules) {
        this.rules = Stream.concat(MANDATORY_RULES.stream(),
                DomainArguments.requireList(additionalRules, "additionalRules").stream()).toList();
    }

    @Override
    public ApprovalResult validate(Expedition expedition) {
        return new ApprovalResult(rules.stream()
                .flatMap(rule -> rule.validate(expedition).stream())
                .toList());
    }
}
