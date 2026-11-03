package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.RegisterZoneRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ZoneResponse;
import ar.edu.itba.dps.fieldops.business.models.responses.ZoneView;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.models.commands.RegisterZoneCommand;

public final class ZoneMapper {
    private ZoneMapper() {
    }

    public static RegisterZoneCommand toCommand(RegisterZoneRequest request) {
        return new RegisterZoneCommand(
                request.id(),
                request.name(),
                Mappings.mapped(request.requiredPermits(), Permit::new));
    }

    public static ZoneResponse toResponse(ZoneView zone) {
        return new ZoneResponse(
                zone.id(),
                zone.name(),
                Mappings.sorted(zone.requiredPermits().stream().map(Permit::name)));
    }
}
