package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionRestrictions;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;

import java.util.List;
import java.util.Set;

public record CreateExpeditionCommand(String name, TimePeriod period, List<String> objectives, Set<String> zoneIds,
                                      Set<ResourceId> responsibleIds, Set<Permit> grantedPermits,
                                      ExpeditionRestrictions restrictions) {
}
