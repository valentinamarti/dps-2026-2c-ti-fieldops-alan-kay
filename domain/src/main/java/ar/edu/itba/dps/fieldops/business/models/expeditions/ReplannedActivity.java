package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.interfaces.resources.Resource;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public record ReplannedActivity(TimePeriod period, Predicate<Resource> keeps, List<ResourceAssignment> additions) {
    public ReplannedActivity {
        Objects.requireNonNull(period, "period is required");
        Objects.requireNonNull(keeps, "keeps is required");
        additions = DomainArguments.requireList(additions, "additions");
    }

    public ReplannedActivity(TimePeriod period, Predicate<Resource> keeps) {
        this(period, keeps, List.of());
    }
}
