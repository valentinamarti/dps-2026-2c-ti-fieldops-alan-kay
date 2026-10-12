package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;

/**
 * The general restrictions of an expedition: how many people it admits and how much risk it tolerates.
 * A missing {@code maxParticipants} arrives as zero and the domain rejects it.
 */
public record RestrictionsDto(int maxParticipants, RiskLevel riskTolerance) {
}
