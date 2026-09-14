package ar.edu.itba.dps.fieldops.business.models.resources;

import java.util.Objects;


public record ResourceCategory(String name) {

    public ResourceCategory {
        Objects.requireNonNull(name, "name is required");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
    }
}
