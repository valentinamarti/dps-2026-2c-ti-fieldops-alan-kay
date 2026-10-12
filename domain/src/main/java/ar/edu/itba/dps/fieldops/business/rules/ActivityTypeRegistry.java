package ar.edu.itba.dps.fieldops.business.rules;

import ar.edu.itba.dps.fieldops.business.exceptions.UnknownActivityTypeException;
import ar.edu.itba.dps.fieldops.business.interfaces.activities.ActivityRules;
import ar.edu.itba.dps.fieldops.business.interfaces.activities.ActivityType;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityParameters;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityTypeName;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ActivityTypeRegistry {

    private final Map<ActivityTypeName, ActivityType> types = new LinkedHashMap<>();

    public ActivityTypeRegistry(List<ActivityType> types) {
        DomainArguments.requireList(types, "types").forEach(this::register);
    }

    public static ActivityTypeRegistry withStandardTypes() {
        return new ActivityTypeRegistry(List.of(
                SampleCollectionRules.TYPE,
                DivingRules.TYPE,
                DroneSurveyRules.TYPE,
                WildlifeObservationRules.TYPE,
                WaterQualityMeasurementRules.TYPE,
                TransferRules.TYPE));
    }

    public List<ActivityType> types() {
        return List.copyOf(types.values());
    }

    public ActivityRules rulesFor(String typeName, Map<String, Integer> parameters, boolean atNight) {
        final var rules = typeNamed(typeName).rulesWith(new ActivityParameters(parameters));
        return atNight ? new NightOperationRules(rules) : rules;
    }

    private ActivityType typeNamed(String typeName) {
        final var name = new ActivityTypeName(typeName);
        return Optional.ofNullable(types.get(name)).orElseThrow(() -> new UnknownActivityTypeException(name, types.keySet()));
    }

    private void register(ActivityType type) {
        if (types.putIfAbsent(type.name(), type) != null) {
            throw new IllegalArgumentException("the activity type %s is registered twice".formatted(type.name()));
        }
    }
}
