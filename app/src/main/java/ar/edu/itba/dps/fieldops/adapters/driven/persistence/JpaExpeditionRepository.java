package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import ar.edu.itba.dps.fieldops.business.exceptions.DuplicateExpeditionException;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;
import ar.edu.itba.dps.fieldops.business.rules.ActivityTypeRegistry;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Transactional
public class JpaExpeditionRepository implements ExpeditionRepository {
    private static final String CACHE_PREFIX = "expedition:";

    private final ExpeditionJpaRepository expeditions;
    private final ExpeditionMapper mapper;
    private final LoadedAggregates loaded;

    public JpaExpeditionRepository(ExpeditionJpaRepository expeditions, ZoneRepository zones, ResourceCatalog catalog,
                                   ActivityTypeRegistry types, LoadedAggregates loaded) {
        this.expeditions = Objects.requireNonNull(expeditions, "expeditions are required");
        this.mapper = new ExpeditionMapper(zones, catalog, types);
        this.loaded = Objects.requireNonNull(loaded, "loaded is required");
    }

    @Override
    public ExpeditionId nextId() {
        return new ExpeditionId(UUID.randomUUID().toString());
    }

    @Override
    public void add(Expedition expedition) {
        Objects.requireNonNull(expedition, "expedition is required");
        if (expeditions.existsById(expedition.getId().value())) {
            throw new DuplicateExpeditionException(expedition.getId());
        }
        save(expedition);
    }

    @Override
    public void save(Expedition expedition) {
        Objects.requireNonNull(expedition, "expedition is required");
        final var entity = expeditions.findById(expedition.getId().value()).orElseGet(ExpeditionEntity::new);
        mapper.fill(entity, expedition.snapshot());
        expeditions.save(entity);
        loaded.put(CACHE_PREFIX + expedition.getId().value(), expedition);
    }

    @Override
    public Optional<Expedition> findById(ExpeditionId id) {
        return expeditions.findById(id.value()).map(this::loadedFrom);
    }

    @Override
    public List<Expedition> findAll() {
        return expeditions.findAllByOrderByCreatedOrderAsc().stream().map(this::loadedFrom).toList();
    }

    private Expedition loadedFrom(ExpeditionEntity entity) {
        final var key = CACHE_PREFIX + entity.getId();
        final Expedition alreadyLoaded = loaded.get(key);
        if (alreadyLoaded != null) {
            return alreadyLoaded;
        }
        final var expedition = mapper.toDomain(entity);
        loaded.put(key, expedition);
        return expedition;
    }
}
