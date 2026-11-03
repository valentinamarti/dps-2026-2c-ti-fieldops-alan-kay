package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Observation;

import java.time.LocalDateTime;

public interface TrackActivityPort {
    void start(ExpeditionId expeditionId, ActivityId activityId, LocalDateTime at);

    void finish(ExpeditionId expeditionId, ActivityId activityId, LocalDateTime at, String result);

    void observe(ExpeditionId expeditionId, ActivityId activityId, Observation observation);
}
