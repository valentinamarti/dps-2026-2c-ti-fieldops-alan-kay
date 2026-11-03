package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.responses.ZoneView;
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
    public ZoneView get(String id) {
        return ZoneView.of(Lookups.zone(zones, id));
    }

    @Override
    public List<ZoneView> list() {
        return zones.findAll().stream().map(ZoneView::of).toList();
    }
}
