package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.zones.Zone;

import java.util.List;

public interface QueryZonesPort {
    Zone get(String id);

    List<Zone> list();
}
