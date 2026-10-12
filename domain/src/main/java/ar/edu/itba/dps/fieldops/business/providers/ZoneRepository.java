package ar.edu.itba.dps.fieldops.business.providers;

import ar.edu.itba.dps.fieldops.business.models.zones.Zone;

import java.util.List;
import java.util.Optional;

public interface ZoneRepository {

    void add(Zone zone);

    Optional<Zone> findById(String id);

    List<Zone> findAll();
}
