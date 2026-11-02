package ar.edu.itba.dps.fieldops.business.fixtures;

import ar.edu.itba.dps.fieldops.business.exceptions.DuplicateZoneException;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FakeZoneRepository implements ZoneRepository {
    private final Map<String, Zone> zones = new LinkedHashMap<>();

    @Override
    public void add(Zone zone) {
        if (zones.putIfAbsent(zone.getId(), zone) != null) {
            throw new DuplicateZoneException(zone.getId());
        }
    }

    @Override
    public Optional<Zone> findById(String id) {
        return Optional.ofNullable(zones.get(id));
    }

    @Override
    public List<Zone> findAll() {
        return List.copyOf(zones.values());
    }
}
