package ar.edu.itba.dps.fieldops.business.exceptions;

import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

public class ExpeditionNotApprovableException extends RuntimeException {

    /**
     * The criticals that blocked the approval, each with its rule and its subject (D32). They are kept and not
     * only joined into the message, so an adapter can list them without parsing text (D58).
     */
    @Getter
    private final List<ValidationResult> criticals;

    public ExpeditionNotApprovableException(List<ValidationResult> criticals) {
        super("The expedition cannot be approved because of: %s".formatted(
                criticals.stream().map(ValidationResult::message).collect(Collectors.joining("; "))));
        this.criticals = List.copyOf(criticals);
    }
}
