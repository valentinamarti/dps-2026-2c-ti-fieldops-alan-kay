package ar.edu.itba.dps.fieldops.business.models.validation;

import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;

import java.util.Objects;

public record ValidationResult(Severity severity, RuleId rule, ValidationSubject subject, String message) {

    public ValidationResult {
        Objects.requireNonNull(severity, "severity is required");
        Objects.requireNonNull(rule, "rule is required");
        Objects.requireNonNull(subject, "subject is required");
        message = DomainArguments.requireText(message, "message");
    }

    public static ValidationResult critical(RuleId rule, ValidationSubject subject, String message) {
        return new ValidationResult(Severity.CRITICAL, rule, subject, message);
    }

    public static ValidationResult warning(RuleId rule, ValidationSubject subject, String message) {
        return new ValidationResult(Severity.WARNING, rule, subject, message);
    }

    public boolean isSameFindingAs(ValidationResult other) {
        return rule.equals(other.rule) && subject.equals(other.subject);
    }
}
