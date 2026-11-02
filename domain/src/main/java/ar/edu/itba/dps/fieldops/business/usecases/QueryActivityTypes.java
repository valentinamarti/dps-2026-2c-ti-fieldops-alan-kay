package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.interfaces.activities.ActivityType;
import ar.edu.itba.dps.fieldops.business.ports.in.QueryActivityTypesPort;
import ar.edu.itba.dps.fieldops.business.rules.ActivityTypeRegistry;

import java.util.List;
import java.util.Objects;

public class QueryActivityTypes implements QueryActivityTypesPort {

    private final ActivityTypeRegistry registry;

    public QueryActivityTypes(ActivityTypeRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry is required");
    }

    @Override
    public List<ActivityType> list() {
        return registry.types();
    }
}
