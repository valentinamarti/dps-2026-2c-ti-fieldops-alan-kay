package ar.edu.itba.dps.fieldops.business.models.estimation;

import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public record ExpeditionEstimate(Duration totalDuration, RiskLevel overallRisk,
                                 Map<ResourceCategory, Quantity> estimatedConsumption) {

    public ExpeditionEstimate {
        Objects.requireNonNull(totalDuration, "total duration is required");
        Objects.requireNonNull(overallRisk, "overall risk is required");
        estimatedConsumption = Map.copyOf(estimatedConsumption);
    }
}
