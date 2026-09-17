package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.interfaces.validation.ValidationRule;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;

import java.util.List;

public class CapacityExceededValidation implements ValidationRule {

    @Override
    public List<ValidationResult> validate(Expedition expedition) {
        final var participants = expedition.participants().size();
        final var maxParticipants = expedition.getRestrictions().maxParticipants();
        if (participants <= maxParticipants) {
            return List.of();
        }
        return List.of(ValidationResult.critical("The expedition has %d participants but allows at most %d"
                .formatted(participants, maxParticipants)));
    }
}
