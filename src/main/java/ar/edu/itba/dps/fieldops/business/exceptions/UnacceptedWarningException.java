package ar.edu.itba.dps.fieldops.business.exceptions;

import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;

public class UnacceptedWarningException extends RuntimeException {

    public UnacceptedWarningException(ValidationResult warning) {
        super("A responsible of the expedition must accept this warning: %s".formatted(warning.message()));
    }
}
