package ar.edu.itba.dps.fieldops.business.models.validation;

import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;

import java.util.Objects;

public record AcceptedWarning(ValidationResult warning, Person responsible, String justification) {

    public AcceptedWarning {
        Objects.requireNonNull(warning, "warning is required");
        if (warning.severity() != Severity.WARNING) {
            throw new IllegalArgumentException("only warnings can be accepted");
        }
        Objects.requireNonNull(responsible, "responsible is required");
        justification = DomainArguments.requireText(justification, "justification");
    }

    public boolean covers(ValidationResult result) {
        return warning.equals(result);
    }
}
