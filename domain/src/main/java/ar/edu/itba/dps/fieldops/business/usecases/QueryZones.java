package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;

import java.util.List;
import java.util.Objects;

public class QueryZones {

    private final ZoneRepository zones;

    public QueryZones(ZoneRepository zones) {
        this.zones = Objects.requireNonNull(zones, "zones are required");
    }

    public Zone get(String id) {
        return Lookups.zone(zones, id);
    }

    public List<Zone> list() {
        return zones.findAll();
    }
}
