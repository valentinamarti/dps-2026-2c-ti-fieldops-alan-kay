package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;

import java.util.Objects;

public record ExpeditionRestrictions(int maxParticipants, RiskLevel riskTolerance) {

    public ExpeditionRestrictions {
        if (maxParticipants <= 0) {
            throw new IllegalArgumentException("max participants must be positive");
        }
        Objects.requireNonNull(riskTolerance, "risk tolerance is required");
    }
}
