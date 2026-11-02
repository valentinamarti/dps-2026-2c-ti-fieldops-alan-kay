package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;

public interface TransitionExpeditionPort {
    void submitForReview(ExpeditionId id);

    void returnToDraft(ExpeditionId id);

    void start(ExpeditionId id);

    void suspend(ExpeditionId id);

    void resume(ExpeditionId id);
}
