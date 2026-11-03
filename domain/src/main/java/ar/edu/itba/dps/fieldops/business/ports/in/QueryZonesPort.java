package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.responses.ZoneView;

import java.util.List;

public interface QueryZonesPort {
    ZoneView get(String id);

    List<ZoneView> list();
}
