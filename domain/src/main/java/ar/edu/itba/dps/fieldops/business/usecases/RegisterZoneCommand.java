package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.zones.Permit;

import java.util.Set;

public record RegisterZoneCommand(String id, String name, Set<Permit> requiredPermits) {
}
