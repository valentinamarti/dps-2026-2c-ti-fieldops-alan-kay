package ar.edu.itba.dps.fieldops.business.models.activities;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public record ActivityParameters(Map<String, Integer> values) {

    public ActivityParameters {
        Objects.requireNonNull(values, "parameters are required");
        values = values.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Optional<Integer> valueOf(String name) {
        return Optional.ofNullable(values.get(name));
    }
}
