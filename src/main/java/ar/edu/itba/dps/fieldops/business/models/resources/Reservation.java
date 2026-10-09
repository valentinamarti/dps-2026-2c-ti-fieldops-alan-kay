package ar.edu.itba.dps.fieldops.business.models.resources;

import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;

import java.util.Objects;

public record Reservation(ExpeditionId owner, TimePeriod period) {

    public Reservation {
        Objects.requireNonNull(owner, "owner is required");
        Objects.requireNonNull(period, "period is required");
    }

    public boolean overlapsWith(TimePeriod other) {
        return period.overlapsWith(other);
    }
}
