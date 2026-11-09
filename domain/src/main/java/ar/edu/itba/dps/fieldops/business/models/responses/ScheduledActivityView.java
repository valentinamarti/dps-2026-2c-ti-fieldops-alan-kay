package ar.edu.itba.dps.fieldops.business.models.responses;

import ar.edu.itba.dps.fieldops.business.interfaces.resources.Resource;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public record ScheduledActivityView(ActivityId id, String name, String zoneId, TimePeriod timeWindow,
                                    TimePeriod scheduledPeriod, List<ActivityId> dependencyIds,
                                    Duration estimatedDuration, RiskLevel estimatedRisk,
                                    List<StaffRequirement> requiredStaff, List<ResourceId> assignedStaffIds,
                                    List<ReusableRequirement> requiredEquipment, List<ResourceId> assignedEquipmentIds,
                                    List<DepletableRequirement> requiredSupplies, List<ResourceId> assignedSupplyIds,
                                    Set<Certification> requiredCertifications, Set<Permit> requiredPermits,
                                    ActivityTrackingView tracking) {

    public static ScheduledActivityView of(ItineraryItem item) {
        final var activity = item.getActivity();
        return new ScheduledActivityView(
                activity.getId(),
                activity.getName(),
                activity.getZone().getId(),
                activity.getTimeWindow(),
                item.getScheduledPeriod(),
                activity.getDependencies().stream().map(dependency -> dependency.getId()).toList(),
                activity.estimatedDuration(),
                activity.estimatedRisk(),
                activity.requiredStaff(),
                idsOf(item.getAssignedStaff()),
                activity.requiredEquipment(),
                idsOf(item.getAssignedEquipment()),
                activity.requiredSupplies(),
                idsOf(item.getAssignedSupplies()),
                activity.requiredCertificationsForAllStaff(),
                activity.requiredPermits(),
                ActivityTrackingView.of(item));
    }

    private static List<ResourceId> idsOf(Collection<? extends Resource> resources) {
        return resources.stream().map(Resource::getId).toList();
    }
}
