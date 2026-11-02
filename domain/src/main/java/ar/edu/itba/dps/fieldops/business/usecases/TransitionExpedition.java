package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.ports.in.TransitionExpeditionPort;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;

import java.util.Objects;
import java.util.function.Consumer;

public class TransitionExpedition implements TransitionExpeditionPort {
    private final ExpeditionRepository repository;

    public TransitionExpedition(ExpeditionRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
    }

    @Override
    public void submitForReview(ExpeditionId id) {
        apply(id, Expedition::submitForReview);
    }

    @Override
    public void returnToDraft(ExpeditionId id) {
        apply(id, Expedition::returnToDraft);
    }

    @Override
    public void start(ExpeditionId id) {
        apply(id, Expedition::start);
    }

    @Override
    public void suspend(ExpeditionId id) {
        apply(id, Expedition::suspend);
    }

    @Override
    public void resume(ExpeditionId id) {
        apply(id, Expedition::resume);
    }

    private void apply(ExpeditionId id, Consumer<Expedition> transition) {
        final var expedition = Lookups.expedition(repository, id);
        transition.accept(expedition);
        repository.save(expedition);
    }
}
