package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import ar.edu.itba.dps.fieldops.business.exceptions.DuplicateZoneException;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
public class JpaZoneRepository implements ZoneRepository {
    private final ZoneJpaRepository zones;

    public JpaZoneRepository(ZoneJpaRepository zones) {
        this.zones = Objects.requireNonNull(zones, "zones are required");
    }

    @Override
    public void add(Zone zone) {
        Objects.requireNonNull(zone, "zone is required");
        if (zones.existsById(zone.getId())) {
            throw new DuplicateZoneException(zone.getId());
        }
        final var entity = new ZoneEntity();
        entity.setId(zone.getId());
        entity.setName(zone.getName());
        zone.getRequiredPermits().forEach(permit -> entity.getPermits().add(permit.name()));
        zones.save(entity);
    }

    @Override
    public Optional<Zone> findById(String id) {
        return zones.findById(id).map(this::toDomain);
    }

    @Override
    public List<Zone> findAll() {
        return zones.findAllByOrderByCreatedOrderAsc().stream().map(this::toDomain).toList();
    }

    private Zone toDomain(ZoneEntity entity) {
        return new Zone(entity.getId(), entity.getName(),
                entity.getPermits().stream().map(Permit::new).collect(Collectors.toUnmodifiableSet()));
    }
}
