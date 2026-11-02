package ar.edu.itba.dps.fieldops.business.models.responses;

import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;

import java.util.Set;

public record ZoneView(String id, String name, Set<Permit> requiredPermits) {

    public static ZoneView of(Zone zone) {
        return new ZoneView(zone.getId(), zone.getName(), zone.getRequiredPermits());
    }
}
