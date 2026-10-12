package ar.edu.itba.dps.fieldops.adapters.driven;

import ar.edu.itba.dps.fieldops.business.exceptions.DuplicateExpeditionException;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class InMemoryExpeditionRepository implements ExpeditionRepository {

    private final Map<ExpeditionId, Expedition> expeditions = new LinkedHashMap<>();

    @Override
    public ExpeditionId nextId() {
        return new ExpeditionId(UUID.randomUUID().toString());
    }

    @Override
    public void add(Expedition expedition) {
        Objects.requireNonNull(expedition, "expedition is required");
        if (expeditions.putIfAbsent(expedition.getId(), expedition) != null) {
            throw new DuplicateExpeditionException(expedition.getId());
        }
    }

    @Override
    public Optional<Expedition> findById(ExpeditionId id) {
        return Optional.ofNullable(expeditions.get(id));
    }

    @Override
    public List<Expedition> findAll() {
        return List.copyOf(expeditions.values());
    }
}
