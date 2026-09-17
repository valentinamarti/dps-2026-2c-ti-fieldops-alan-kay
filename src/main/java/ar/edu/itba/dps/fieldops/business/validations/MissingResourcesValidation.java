package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.interfaces.validation.ValidationRule;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;

import java.util.List;
import java.util.stream.Stream;

public class MissingResourcesValidation implements ValidationRule {

    @Override
    public List<ValidationResult> validate(Expedition expedition) {
        return expedition.getItinerary().getItems().stream()
                .flatMap(item -> Stream.concat(missingStaff(item), missingEquipment(item)))
                .toList();
    }

    private Stream<ValidationResult> missingStaff(ItineraryItem item) {
        final var required = item.getActivity().requiredStaff().stream().mapToInt(StaffRequirement::headcount).sum();
        final var assigned = item.getAssignedStaff().size();
        if (assigned >= required) {
            return Stream.empty();
        }
        return Stream.of(ValidationResult.critical("%s has %d of the %d staff members it needs"
                .formatted(item.getActivity().getName(), assigned, required)));
    }

    private Stream<ValidationResult> missingEquipment(ItineraryItem item) {
        return item.getActivity().requiredEquipment().stream()
                .filter(requirement -> accepted(item, requirement) < requirement.count())
                .map(requirement -> ValidationResult.critical("%s has %d of the %d %s it needs".formatted(
                        item.getActivity().getName(), accepted(item, requirement), requirement.count(), requirement.category().name())));
    }

    private long accepted(ItineraryItem item, ReusableRequirement requirement) {
        return item.getAssignedEquipment().stream().filter(requirement::accepts).count();
    }
}