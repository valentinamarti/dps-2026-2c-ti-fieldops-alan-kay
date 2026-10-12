package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Observation;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;

import java.time.LocalDateTime;
import java.util.Objects;

public class TrackActivity {

    private final ExpeditionRepository repository;

    public TrackActivity(ExpeditionRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
    }

    public void start(ExpeditionId expeditionId, ActivityId activityId, LocalDateTime at) {
        final var expedition = Lookups.expedition(repository, expeditionId);
        expedition.startActivity(Lookups.item(expedition, activityId), at);
    }

    public void finish(ExpeditionId expeditionId, ActivityId activityId, LocalDateTime at, String result) {
        final var expedition = Lookups.expedition(repository, expeditionId);
        expedition.finishActivity(Lookups.item(expedition, activityId), at, result);
    }

    public void observe(ExpeditionId expeditionId, ActivityId activityId, Observation observation) {
        final var expedition = Lookups.expedition(repository, expeditionId);
        expedition.recordObservation(Lookups.item(expedition, activityId), observation);
    }
}
