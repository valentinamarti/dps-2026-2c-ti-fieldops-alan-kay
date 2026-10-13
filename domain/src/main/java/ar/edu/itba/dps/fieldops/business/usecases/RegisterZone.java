package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.DuplicateZoneException;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;

import java.util.Objects;

public class RegisterZone {
    private final ZoneRepository zones;

    public RegisterZone(ZoneRepository zones) {
        this.zones = Objects.requireNonNull(zones, "zones are required");
    }

    public Zone register(RegisterZoneCommand command) {
        final var zone = new Zone(command.id(), command.name(), command.requiredPermits());
        zones.add(zone);
        return zone;
    }
}
