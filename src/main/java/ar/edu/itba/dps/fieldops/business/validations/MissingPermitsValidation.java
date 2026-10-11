package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.interfaces.validation.ValidationRule;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.validation.RuleId;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationSubject;

import java.util.List;

public class MissingPermitsValidation implements ValidationRule {

    public static final RuleId ID = new RuleId("missing-permits");

    @Override
    public List<ValidationResult> validate(Expedition expedition) {
        return expedition.getZones().stream()
                .flatMap(zone -> zone.missingPermits(expedition.getGrantedPermits()).stream()
                        .map(permit -> ValidationResult.critical(ID, ValidationSubject.expedition(), "%s requires the permit %s, which was not granted"
                                .formatted(zone.getName(), permit.name()))))
                .toList();
    }
}
