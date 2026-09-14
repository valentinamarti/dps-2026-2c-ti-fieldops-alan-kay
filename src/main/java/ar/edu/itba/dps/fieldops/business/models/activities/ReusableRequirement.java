package ar.edu.itba.dps.fieldops.business.models.activities;

import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;

import java.util.Objects;


public record ReusableRequirement(ResourceCategory category, int count) {

    public ReusableRequirement {
        Objects.requireNonNull(category, "category is required");
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive");
        }
    }
}
