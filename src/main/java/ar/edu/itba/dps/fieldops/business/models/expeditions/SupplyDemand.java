package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.interfaces.resources.DepletableResource;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;

import java.util.List;
import java.util.Objects;

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
}
