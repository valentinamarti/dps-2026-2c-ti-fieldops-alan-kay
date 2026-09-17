package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.interfaces.resources.DepletableResource;
import ar.edu.itba.dps.fieldops.business.interfaces.validation.ValidationRule;
import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class InsufficientSuppliesValidation implements ValidationRule {

    @Override
    public List<ValidationResult> validate(Expedition expedition) {
        final var items = expedition.getItinerary().getItems();
        return Stream.concat(items.stream().flatMap(this::uncoveredRequirements), insufficientStock(items)).toList();
    }

    private Stream<ValidationResult> uncoveredRequirements(ItineraryItem item) {
        return item.getActivity().requiredSupplies().stream()
                .filter(requirement -> supplyFor(item, requirement).isEmpty())
                .map(requirement -> ValidationResult.critical("%s needs %s of %s but has no supply of that category assigned"
                        .formatted(item.getActivity().getName(), requirement.quantity(), requirement.category().name())));
    }

    private Stream<ValidationResult> insufficientStock(List<ItineraryItem> items) {
        return demandPerSupply(items).entrySet().stream()
                .filter(demand -> !demand.getKey().hasStockFor(demand.getValue()))
                .map(demand -> ValidationResult.critical("%s does not have enough stock for the %s the itinerary needs"
                        .formatted(demand.getKey().getId(), demand.getValue())));
    }

    private Map<DepletableResource, Quantity> demandPerSupply(List<ItineraryItem> items) {
        final var demand = new LinkedHashMap<DepletableResource, Quantity>();
        items.forEach(item -> item.getActivity().requiredSupplies().forEach(requirement ->
                supplyFor(item, requirement).ifPresent(supply -> demand.merge(supply, requirement.quantity(), Quantity::plus))));
        return demand;
    }

    private Optional<DepletableResource> supplyFor(ItineraryItem item, DepletableRequirement requirement) {
        return item.getAssignedSupplies().stream().filter(requirement::accepts).findFirst();
    }
}