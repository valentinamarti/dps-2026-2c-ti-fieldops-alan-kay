package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;
import java.util.Set;

/**
 * The body of {@code POST /api/expeditions}. Zones and responsibles are referenced by id, like the command the
 * use case takes (D38); nothing here is validated by the adapter, because the domain already does it.
 */
public record CreateExpeditionRequest(String name, PeriodDto period, List<String> objectives, Set<String> zoneIds,
                                      Set<String> responsibleIds, Set<String> grantedPermits,
                                      RestrictionsDto restrictions) {
}
