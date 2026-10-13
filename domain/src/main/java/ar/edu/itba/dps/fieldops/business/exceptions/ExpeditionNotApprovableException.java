package ar.edu.itba.dps.fieldops.business.exceptions;

import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

public class ExpeditionNotApprovableException extends RuntimeException {
    @Getter
    private final List<ValidationResult> criticals;

    public ExpeditionNotApprovableException(List<ValidationResult> criticals) {
        super("The expedition cannot be approved because of: %s".formatted(
                criticals.stream().map(ValidationResult::message).collect(Collectors.joining("; "))));
        this.criticals = List.copyOf(criticals);
    }
}
