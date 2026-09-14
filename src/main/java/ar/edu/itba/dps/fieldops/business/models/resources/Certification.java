package ar.edu.itba.dps.fieldops.business.models.resources;

import java.util.Objects;

public record Certification(String name) {

    // TODO: review when the teacher replies to our questions - maybe new fields? maybe certification type enum?
    public Certification {
        Objects.requireNonNull(name, "name is required");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
    }
}
