package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.interfaces.resources.DepletableResource;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public record SupplyDemand(DepletableResource supply, List<Quantity> quantities) {

    public SupplyDemand {
        Objects.requireNonNull(supply, "supply is required");
        quantities = DomainArguments.requireNonEmptyList(quantities, "quantities");
    }

    public boolean isInSupplyUnit() {
        return quantities.stream().allMatch(quantity -> quantity.unit() == supply.unit());
    }

    public Quantity total() {
        return quantities.stream().reduce(Quantity::plus).orElseThrow();
    }

    public static List<SupplyDemand> combine(Stream<SupplyDemand> demands) {
        final var quantitiesPerSupply = new LinkedHashMap<DepletableResource, List<Quantity>>();
        demands.forEach(demand ->
                quantitiesPerSupply.computeIfAbsent(demand.supply(), _ -> new ArrayList<>()).addAll(demand.quantities()));
        return quantitiesPerSupply.entrySet().stream()
                .map(entry -> new SupplyDemand(entry.getKey(), entry.getValue()))
                .toList();
    }
}
