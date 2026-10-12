package ar.edu.itba.dps.fieldops.business.rules;

import ar.edu.itba.dps.fieldops.business.exceptions.InvalidActivityParametersException;
import ar.edu.itba.dps.fieldops.business.interfaces.activities.ActivityRules;
import ar.edu.itba.dps.fieldops.business.interfaces.activities.ActivityType;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityParameters;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityTypeName;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;

import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;

final class SingleParameterActivityType implements ActivityType {

    private final ActivityTypeName name;
    private final String parameter;
    private final IntFunction<ActivityRules> rules;

    SingleParameterActivityType(String name, String parameter, IntFunction<ActivityRules> rules) {
        this.name = new ActivityTypeName(name);
        this.parameter = DomainArguments.requireText(parameter, "parameter");
        this.rules = Objects.requireNonNull(rules, "rules are required");
    }

    @Override
    public ActivityTypeName name() {
        return name;
    }

    @Override
    public List<String> parameterNames() {
        return List.of(parameter);
    }

    @Override
    public ActivityRules rulesWith(ActivityParameters parameters) {
        final int value = parameters.valueOf(parameter).orElseThrow(() -> new InvalidActivityParametersException(
                name, "the parameter %s is required".formatted(parameter)));
        try {
            return rules.apply(value);
        } catch (IllegalArgumentException invalid) {
            throw new InvalidActivityParametersException(
                    name, "%s cannot be %d, %s".formatted(parameter, value, invalid.getMessage()));
        }
    }
}
