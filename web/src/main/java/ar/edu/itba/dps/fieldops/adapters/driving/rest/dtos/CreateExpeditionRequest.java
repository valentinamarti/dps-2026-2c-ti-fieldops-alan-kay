package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;
import java.util.Set;

public record CreateExpeditionRequest(String name, PeriodDto period, List<String> objectives, Set<String> zoneIds,
                                      Set<String> responsibleIds, Set<String> grantedPermits,
                                      RestrictionsDto restrictions) {
}
