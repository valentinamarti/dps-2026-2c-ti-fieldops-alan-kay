package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * The status changes that only touch the expedition itself. Approving and finishing also move reservations,
 * so they have their own use cases.
 */
public class TransitionExpedition {

    private final ExpeditionRepository repository;

    public TransitionExpedition(ExpeditionRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
    }

    public void submitForReview(ExpeditionId id) {
        apply(id, Expedition::submitForReview);
    }

    public void returnToDraft(ExpeditionId id) {
        apply(id, Expedition::returnToDraft);
    }

    public void start(ExpeditionId id) {
        apply(id, Expedition::start);
    }

    public void suspend(ExpeditionId id) {
        apply(id, Expedition::suspend);
    }

    public void resume(ExpeditionId id) {
        apply(id, Expedition::resume);
    }

    private void apply(ExpeditionId id, Consumer<Expedition> transition) {
        transition.accept(Lookups.expedition(repository, id));
    }
}
