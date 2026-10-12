package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.RegisterZoneRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ZoneResponse;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.usecases.RegisterZoneCommand;

/**
 * Translates between the zone DTOs and the domain, in both directions (D49).
 */
public final class ZoneMapper {

    private ZoneMapper() {
    }

    public static RegisterZoneCommand toCommand(RegisterZoneRequest request) {
        return new RegisterZoneCommand(
                request.id(),
                request.name(),
                Mappings.mapped(request.requiredPermits(), Permit::new));
    }

    public static ZoneResponse toResponse(Zone zone) {
        return new ZoneResponse(
                zone.getId(),
                zone.getName(),
                Mappings.sorted(zone.getRequiredPermits().stream().map(Permit::name)));
    }
}
