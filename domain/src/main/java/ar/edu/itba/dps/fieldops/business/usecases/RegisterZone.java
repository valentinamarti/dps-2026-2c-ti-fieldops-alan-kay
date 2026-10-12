package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.DuplicateZoneException;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;

import java.util.Objects;

/**
 * Registers a zone so the expeditions can reference it by id. It is split from {@link QueryZones} for the same
 * reason {@link CreateExpedition} is split from {@link QueryExpeditions}: writing needs the repository to
 * reject a repeated id ({@link DuplicateZoneException}, part of the contract of every {@code ZoneRepository}),
 * reading does not.
 */
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
