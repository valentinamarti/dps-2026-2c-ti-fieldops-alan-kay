package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;

import java.util.List;

public record ExpeditionResponse(String id, String name, ExpeditionStatus status, PeriodDto period,
                                 List<String> objectives, List<String> zoneIds, List<String> responsibleIds,
                                 List<String> grantedPermits, RestrictionsDto restrictions,
                                 String replacedExpeditionId, String replacementId) {
}
