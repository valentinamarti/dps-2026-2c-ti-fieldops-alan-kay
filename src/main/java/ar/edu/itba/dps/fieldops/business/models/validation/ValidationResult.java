package ar.edu.itba.dps.fieldops.business.models.validation;

import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;

import java.util.Objects;

public record ValidationResult(Severity severity, String message) {

    public ValidationResult {
        Objects.requireNonNull(severity, "severity is required");
        message = DomainArguments.requireText(message, "message");
    }

    public static ValidationResult critical(String message) {
        return new ValidationResult(Severity.CRITICAL, message);
    }

    public static ValidationResult warning(String message) {
        return new ValidationResult(Severity.WARNING, message);
    }
}
