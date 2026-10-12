package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.interfaces.validation.ValidationRule;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.validation.RuleId;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationSubject;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MissingCertificationsValidation implements ValidationRule {

    public static final RuleId ID = new RuleId("missing-certifications");

    @Override
    public List<ValidationResult> validate(Expedition expedition) {
        return expedition.getItinerary().getItems().stream()
                .flatMap(item -> Stream.concat(unqualifiedRequirements(item), uncertifiedStaff(item)))
                .toList();
    }

    private Stream<ValidationResult> unqualifiedRequirements(ItineraryItem item) {
        return item.getActivity().requiredStaff().stream()
                .filter(requirement -> !requirement.certifications().isEmpty())
                .map(requirement -> new Shortage(requirement, qualified(item, requirement)))
                .filter(Shortage::isShortage)
                .map(shortage -> ValidationResult.critical(ID, ValidationSubject.of(item.getActivity().getId()), "%s needs %d people certified in %s but only %d qualify".formatted(
                        item.getActivity().getName(), shortage.requirement().headcount(),
                        names(shortage.requirement().certifications()), shortage.qualified())));
    }

    private Stream<ValidationResult> uncertifiedStaff(ItineraryItem item) {
        final var activity = item.getActivity();
        return item.getAssignedStaff().stream()
                .filter(person -> !activity.admits(person))
                .map(person -> ValidationResult.critical(ID, ValidationSubject.of(activity.getId(), person.getId()), "%s requires everyone assigned to be certified in %s, but %s is not".formatted(
                        activity.getName(), names(activity.requiredCertificationsForAllStaff()), person.getId())));
    }

    private record Shortage(StaffRequirement requirement, long qualified) {

        boolean isShortage() {
            return qualified < requirement.headcount();
        }
    }

    private long qualified(ItineraryItem item, StaffRequirement requirement) {
        return item.getAssignedStaff().stream().filter(requirement::qualifies).count();
    }

    private String names(Set<Certification> certifications) {
        return certifications.stream().map(Certification::name).sorted().collect(Collectors.joining(", "));
    }
}
