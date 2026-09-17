package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.interfaces.validation.ValidationRule;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;

import java.util.List;

public class MissingPermitsValidation implements ValidationRule {

    @Override
    public List<ValidationResult> validate(Expedition expedition) {
        return expedition.getZones().stream()
                .flatMap(zone -> zone.missingPermits(expedition.getGrantedPermits()).stream()
                        .map(permit -> ValidationResult.critical("%s requires the permit %s, which was not granted"
                                .formatted(zone.getName(), permit.name()))))
                .toList();
    }
}
