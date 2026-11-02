package ar.edu.itba.dps.fieldops.business.ports.in;

import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;

public interface AssignResourcesPort {
    void assignStaff(ExpeditionId expeditionId, ActivityId activityId, ResourceId personId);

    void assignEquipment(ExpeditionId expeditionId, ActivityId activityId, ResourceId equipmentId);

    void assignSupply(ExpeditionId expeditionId, ActivityId activityId, ResourceId supplyId);
}
