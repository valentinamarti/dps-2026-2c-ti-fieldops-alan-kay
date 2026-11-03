package ar.edu.itba.dps.fieldops.business.models.activities;

import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;

import java.util.Map;
import java.util.Objects;

public record ActivityDefinition(String typeName, Map<String, Integer> parameters, boolean atNight) {

    public ActivityDefinition {
        typeName = DomainArguments.requireText(typeName, "typeName");
        parameters = Map.copyOf(Objects.requireNonNull(parameters, "parameters are required"));
    }

    public ActivityDefinition asNightOperation() {
        return new ActivityDefinition(typeName, parameters, true);
    }
}
