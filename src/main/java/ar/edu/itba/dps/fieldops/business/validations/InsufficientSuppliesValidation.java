package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.interfaces.resources.DepletableResource;
import ar.edu.itba.dps.fieldops.business.interfaces.validation.ValidationRule;
import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;

import java.util.ArrayList;
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
                .flatMap(demand -> stockResults(demand.getKey(), demand.getValue()));
    }

    private Stream<ValidationResult> stockResults(DepletableResource supply, List<Quantity> demanded) {
        if (demanded.stream().anyMatch(quantity -> quantity.unit() != supply.unit())) {
            return Stream.of(ValidationResult.critical("%s is measured in %s, but the itinerary asks for it in another unit"
                    .formatted(supply.getId(), supply.unit())));
        }
        final var total = demanded.stream().reduce(Quantity::plus).orElseThrow();
        if (supply.hasStockFor(total)) {
            return Stream.empty();
        }
        return Stream.of(ValidationResult.critical("%s does not have enough stock for the %s the itinerary needs"
                .formatted(supply.getId(), total)));
    }

    private Map<DepletableResource, List<Quantity>> demandPerSupply(List<ItineraryItem> items) {
        final var demand = new LinkedHashMap<DepletableResource, List<Quantity>>();
        items.forEach(item -> item.getActivity().requiredSupplies().forEach(requirement ->
                supplyFor(item, requirement).ifPresent(supply ->
                        demand.computeIfAbsent(supply, key -> new ArrayList<>()).add(requirement.quantity()))));
        return demand;
    }

    private Optional<DepletableResource> supplyFor(ItineraryItem item, DepletableRequirement requirement) {
        return item.getAssignedSupplies().stream().filter(requirement::accepts).findFirst();
    }
}