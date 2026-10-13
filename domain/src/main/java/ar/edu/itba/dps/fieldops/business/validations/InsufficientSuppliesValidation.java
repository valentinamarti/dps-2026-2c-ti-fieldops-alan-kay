package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.interfaces.validation.ValidationRule;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.expeditions.SupplyDemand;
import ar.edu.itba.dps.fieldops.business.models.validation.RuleId;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationSubject;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class InsufficientSuppliesValidation implements ValidationRule {

    public static final RuleId ID = new RuleId("insufficient-supplies");

    @Override
    public List<ValidationResult> validate(Expedition expedition) {
        final var itinerary = expedition.getItinerary();
        return Stream.concat(
                itinerary.getItems().stream().flatMap(this::uncoveredRequirements),
                itinerary.supplyDemands().stream().flatMap(demand -> stockResults(demand, expedition.reservationsItDoesNotCompeteWith()))).toList();
    }

    private Stream<ValidationResult> uncoveredRequirements(ItineraryItem item) {
        return item.getActivity().requiredSupplies().stream()
                .filter(requirement -> item.supplyFor(requirement).isEmpty())
                .map(requirement -> ValidationResult.critical(ID, ValidationSubject.of(item.getActivity().getId()), "%s needs %s of %s but has no supply of that category assigned"
                        .formatted(item.getActivity().getName(), requirement.quantity(), requirement.category().name())));
    }

    private Stream<ValidationResult> stockResults(SupplyDemand demand, Set<ExpeditionId> ignoredOwners) {
        final var supply = demand.supply();
        if (!demand.isInSupplyUnit()) {
            return Stream.of(ValidationResult.critical(ID, ValidationSubject.of(supply.getId()), "%s is measured in %s, but the itinerary asks for it in another unit"
                    .formatted(supply.getId(), supply.unit())));
        }
        if (supply.hasStockFor(demand.total(), ignoredOwners)) {
            return Stream.empty();
        }
        return Stream.of(ValidationResult.critical(ID, ValidationSubject.of(supply.getId()), "%s does not have enough stock for the %s the itinerary needs"
                .formatted(supply.getId(), demand.total())));
    }
}
