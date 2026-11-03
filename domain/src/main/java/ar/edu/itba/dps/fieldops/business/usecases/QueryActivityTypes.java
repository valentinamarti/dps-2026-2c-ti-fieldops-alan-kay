package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.responses.ActivityTypeView;
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
    public List<ActivityTypeView> list() {
        return registry.types().stream().map(ActivityTypeView::of).toList();
    }
}
