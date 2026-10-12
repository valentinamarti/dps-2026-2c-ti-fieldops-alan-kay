package ar.edu.itba.dps.fieldops.business.interfaces.validation;

import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;

import java.util.List;

public interface ValidationRule {

    List<ValidationResult> validate(Expedition expedition);
}
