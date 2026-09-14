package ar.edu.itba.dps.fieldops.business.models.zones;

import java.util.Objects;

public record Permit(String name) {

    public Permit {
        Objects.requireNonNull(name, "name is required");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
    }
}
