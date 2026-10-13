package ar.edu.itba.dps.fieldops.business.fixtures;

import ar.edu.itba.dps.fieldops.business.exceptions.DuplicateExpeditionException;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SequentialExpeditionRepository implements ExpeditionRepository {
    private final Map<ExpeditionId, Expedition> expeditions = new LinkedHashMap<>();
    private int issued = 0;

    @Override
    public ExpeditionId nextId() {
        issued++;
        return new ExpeditionId("e-" + issued);
    }

    @Override
    public void add(Expedition expedition) {
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
