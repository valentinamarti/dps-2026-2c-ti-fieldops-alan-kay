package ar.edu.itba.dps.fieldops.business.services;

import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.providers.EventPublisher;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PublishingExpeditionRepository implements ExpeditionRepository {
    private final ExpeditionRepository expeditions;
    private final EventPublisher publisher;

    public PublishingExpeditionRepository(ExpeditionRepository expeditions, EventPublisher publisher) {
        this.expeditions = Objects.requireNonNull(expeditions, "expeditions are required");
        this.publisher = Objects.requireNonNull(publisher, "publisher is required");
    }

    @Override
    public ExpeditionId nextId() {
        return expeditions.nextId();
    }

    @Override
    public void add(Expedition expedition) {
        expeditions.add(expedition);
        publishTheEventsOf(expedition);
    }

    @Override
    public void save(Expedition expedition) {
        expeditions.save(expedition);
        publishTheEventsOf(expedition);
    }

    @Override
    public Optional<Expedition> findById(ExpeditionId id) {
        return expeditions.findById(id);
    }

    @Override
    public List<Expedition> findAll() {
        return expeditions.findAll();
    }

    private void publishTheEventsOf(Expedition expedition) {
        expedition.pullEvents().forEach(publisher::publish);
    }
}
