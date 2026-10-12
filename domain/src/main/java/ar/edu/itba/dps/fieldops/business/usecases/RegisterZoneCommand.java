package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.zones.Permit;

import java.util.Set;

/**
 * Everything needed to register a zone. The id is the one the expeditions reference (D38).
 */
public record RegisterZoneCommand(String id, String name, Set<Permit> requiredPermits) {
}
