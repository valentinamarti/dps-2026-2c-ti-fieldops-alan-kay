package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.interfaces.validation.ValidationRule;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;

import java.util.ArrayList;
import java.util.List;

public class ResourceOverlapValidation implements ValidationRule {

    @Override
    public List<ValidationResult> validate(Expedition expedition) {
        final var items = expedition.getItinerary().getItems();
        final var results = new ArrayList<ValidationResult>();
        for (int i = 0; i < items.size(); i++) {
            final var item = items.get(i);
            items.subList(i + 1, items.size()).forEach(later -> results.addAll(sharedResources(item, later)));
            results.addAll(alreadyBookedResources(item));
        }
        return results;
    }

    private List<ValidationResult> sharedResources(ItineraryItem item, ItineraryItem other) {
        if (!item.overlapsWith(other)) {
            return List.of();
        }
        return item.getAssignedReusableResources().stream()
                .filter(other.getAssignedReusableResources()::contains)
                .map(resource -> ValidationResult.critical("%s is assigned to %s and %s at the same time"
                        .formatted(resource.getId(), item.getActivity().getName(), other.getActivity().getName())))
                .toList();
    }

    private List<ValidationResult> alreadyBookedResources(ItineraryItem item) {
        return item.getAssignedReusableResources().stream()
                .filter(resource -> !resource.isAvailableDuring(item.getScheduledPeriod()))
                .map(resource -> ValidationResult.critical("%s is already booked during %s"
                        .formatted(resource.getId(), item.getActivity().getName())))
                .toList();
    }
}
