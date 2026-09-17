package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.interfaces.validation.ValidationRule;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MissingCertificationsValidation implements ValidationRule {

    @Override
    public List<ValidationResult> validate(Expedition expedition) {
        return expedition.getItinerary().getItems().stream()
                .flatMap(this::unqualifiedRequirements)
                .toList();
    }

    private Stream<ValidationResult> unqualifiedRequirements(ItineraryItem item) {
        return item.getActivity().requiredStaff().stream()
                .filter(requirement -> !requirement.certifications().isEmpty())
                .filter(requirement -> qualified(item, requirement) < requirement.headcount())
                .map(requirement -> ValidationResult.critical("%s needs %d people certified in %s but only %d qualify".formatted(
                        item.getActivity().getName(), requirement.headcount(), names(requirement), qualified(item, requirement))));
    }

    private long qualified(ItineraryItem item, StaffRequirement requirement) {
        return item.getAssignedStaff().stream().filter(requirement::qualifies).count();
    }

    private String names(StaffRequirement requirement) {
        return requirement.certifications().stream().map(Certification::name).sorted().collect(Collectors.joining(", "));
    }
}
