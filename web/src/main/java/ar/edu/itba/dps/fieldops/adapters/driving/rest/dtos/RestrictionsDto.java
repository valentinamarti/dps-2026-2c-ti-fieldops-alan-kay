package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;

public record RestrictionsDto(int maxParticipants, RiskLevel riskTolerance) {
}
