package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;

import java.util.Objects;

public record Unavailability(TimePeriod period, String reason) {

    public Unavailability {
        Objects.requireNonNull(period, "period is required");
        reason = DomainArguments.requireText(reason, "reason");
    }

    public boolean overlapsWith(TimePeriod other) {
        return period.overlapsWith(other);
    }
}
