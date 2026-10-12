package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.interfaces.validation.ValidationRule;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.validation.RuleId;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationSubject;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class MissingPermitsValidation implements ValidationRule {

    public static final RuleId ID = new RuleId("missing-permits");

    private static final String NOT_GRANTED = "%s requires the permit %s, which was not granted";

    @Override
    public List<ValidationResult> validate(Expedition expedition) {
        return Stream.concat(zonePermits(expedition), activityPermits(expedition)).toList();
    }

    private Stream<ValidationResult> zonePermits(Expedition expedition) {
        return expedition.getZones().stream()
                .flatMap(zone -> zone.missingPermits(expedition.getGrantedPermits()).stream()
                        .map(permit -> ValidationResult.critical(ID, ValidationSubject.expedition(), NOT_GRANTED
                                .formatted(zone.getName(), permit.name()))));
    }

    private Stream<ValidationResult> activityPermits(Expedition expedition) {
        return expedition.getItinerary().pendingItems().stream()
                .map(ItineraryItem::getActivity)
                .flatMap(activity -> missingPermits(activity, expedition));
    }

    private Stream<ValidationResult> missingPermits(Activity activity, Expedition expedition) {
        return activity.missingPermits(expedition.getGrantedPermits()).stream()
                .sorted(Comparator.comparing(Permit::name))
                .map(permit -> ValidationResult.critical(ID, ValidationSubject.of(activity.getId()), NOT_GRANTED
                        .formatted(activity.getName(), permit.name())));
    }
}
