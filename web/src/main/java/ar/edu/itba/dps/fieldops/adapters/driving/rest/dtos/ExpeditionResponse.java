package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;

import java.util.List;

/**
 * An expedition as the API shows it. The itinerary is not here: the tree has its own endpoint and how it is
 * serialized is still open (D49). The id collections come sorted, so the same expedition always reads the same.
 */
public record ExpeditionResponse(String id, String name, ExpeditionStatus status, PeriodDto period,
                                 List<String> objectives, List<String> zoneIds, List<String> responsibleIds,
                                 List<String> grantedPermits, RestrictionsDto restrictions,
                                 String replacedExpeditionId, String replacementId) {
}
