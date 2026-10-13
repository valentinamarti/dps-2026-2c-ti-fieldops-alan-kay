package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.interfaces.validation.ValidationRule;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Itinerary;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.validation.RuleId;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationSubject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ResourceOverlapValidation implements ValidationRule {

    public static final RuleId ID = new RuleId("resource-overlap");

    private static final String AT_THE_SAME_TIME = "%s is assigned to %s and %s at the same time";
    private static final String IN_PARALLEL = "%s is assigned to %s and %s, which run in parallel in the same block";

    @Override
    public List<ValidationResult> validate(Expedition expedition) {
        final var itinerary = expedition.getItinerary();
        final var items = itinerary.pendingItems();
        final var ignoredOwners = expedition.reservationsItDoesNotCompeteWith();
        final var results = new ArrayList<ValidationResult>();
        for (int i = 0; i < items.size(); i++) {
            final var item = items.get(i);
            items.subList(i + 1, items.size()).forEach(later -> results.addAll(sharedResources(itinerary, item, later)));
            results.addAll(unavailableResources(item, ignoredOwners));
        }
        return results;
    }

    private List<ValidationResult> sharedResources(Itinerary itinerary, ItineraryItem item, ItineraryItem other) {
        if (item.overlapsWith(other)) {
            return sharedResources(item, other, AT_THE_SAME_TIME);
        }
        if (itinerary.runInParallel(item, other)) {
            return sharedResources(item, other, IN_PARALLEL);
        }
        return List.of();
    }

    private List<ValidationResult> sharedResources(ItineraryItem item, ItineraryItem other, String message) {
        return item.getAssignedReusableResources().stream()
                .filter(other.getAssignedReusableResources()::contains)
                .map(resource -> ValidationResult.critical(ID, ValidationSubject.of(item.getActivity().getId(), resource.getId()), message
                        .formatted(resource.getId(), item.getActivity().getName(), other.getActivity().getName())))
                .toList();
    }

    private List<ValidationResult> unavailableResources(ItineraryItem item, Set<ExpeditionId> ignoredOwners) {
        return item.getAssignedReusableResources().stream()
                .filter(resource -> !resource.isAvailableDuring(item.getScheduledPeriod(), ignoredOwners))
                .map(resource -> ValidationResult.critical(ID, ValidationSubject.of(item.getActivity().getId(), resource.getId()), "%s is not available during %s"
                        .formatted(resource.getId(), item.getActivity().getName())))
                .toList();
    }
}
