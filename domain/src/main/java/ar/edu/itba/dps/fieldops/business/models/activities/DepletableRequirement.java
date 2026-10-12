package ar.edu.itba.dps.fieldops.business.models.activities;

import ar.edu.itba.dps.fieldops.business.interfaces.resources.Categorized;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.DepletableResource;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;

import java.util.Objects;

public record DepletableRequirement(ResourceCategory category, Quantity quantity) {

    public DepletableRequirement {
        Objects.requireNonNull(category, "category is required");
        Objects.requireNonNull(quantity, "quantity is required");
        if (quantity.amount().signum() == 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }

    public boolean accepts(Categorized candidate) {
        return candidate.belongsTo(category);
    }

    public boolean isCoveredBy(DepletableResource supply) {
        return accepts(supply) && supply.hasStockFor(quantity);
    }
}
