package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.interfaces.validation.ValidationRule;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;

import java.util.List;

public class RiskToleranceValidation implements ValidationRule {

    @Override
    public List<ValidationResult> validate(Expedition expedition) {
        final var tolerance = expedition.getRestrictions().riskTolerance();
        return expedition.getItinerary().getItems().stream()
                .map(ItineraryItem::getActivity)
                .filter(activity -> activity.estimatedRisk().isAbove(tolerance))
                .map(activity -> ValidationResult.warning("%s has %s risk, above the expedition tolerance of %s"
                        .formatted(activity.getName(), activity.estimatedRisk(), tolerance)))
                .toList();
    }
}
