package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.ports.in.QueryZonesPort;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;

import java.util.List;
import java.util.Objects;

public class QueryZones implements QueryZonesPort {

    private final ZoneRepository zones;

    public QueryZones(ZoneRepository zones) {
        this.zones = Objects.requireNonNull(zones, "zones are required");
    }

    @Override
    public Zone get(String id) {
        return Lookups.zone(zones, id);
    }

    @Override
    public List<Zone> list() {
        return zones.findAll();
    }
}
