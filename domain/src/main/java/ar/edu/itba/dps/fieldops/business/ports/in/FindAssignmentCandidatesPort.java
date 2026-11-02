package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.responses.AssignmentCandidates;

public interface FindAssignmentCandidatesPort {
    AssignmentCandidates candidatesFor(ExpeditionId expeditionId, ActivityId activityId);
}
