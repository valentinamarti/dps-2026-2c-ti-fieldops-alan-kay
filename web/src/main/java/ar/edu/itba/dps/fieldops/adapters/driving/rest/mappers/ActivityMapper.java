package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.EquipmentRequirementDto;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ObservationDto;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ScheduleActivityRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ScheduledActivityResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.StaffRequirementDto;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.SupplyRequirementDto;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.TrackingResponse;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.responses.ActivityTrackingView;
import ar.edu.itba.dps.fieldops.business.models.responses.ScheduledActivityView;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.models.commands.PlannedActivityCommand;
import ar.edu.itba.dps.fieldops.business.models.commands.ScheduleActivityCommand;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ActivityMapper {
    private ActivityMapper() {
    }

    public static ScheduleActivityCommand toCommand(String expeditionId, ScheduleActivityRequest request) {
        return new ScheduleActivityCommand(new ExpeditionId(expeditionId), toPlannedActivity(request));
    }

    static PlannedActivityCommand toPlannedActivity(ScheduleActivityRequest request) {
        return new PlannedActivityCommand(
                new ActivityId(request.id()),
                request.name(),
                Mappings.toPeriod(request.timeWindow()),
                Mappings.toPeriod(request.scheduledPeriod()),
                request.zoneId(),
                Mappings.mappedInOrder(request.dependencyIds(), ActivityId::new),
                request.type(),
                Objects.requireNonNullElse(request.parameters(), Map.of()),
                request.atNight());
    }

    public static ScheduledActivityResponse toResponse(ScheduledActivityView activity) {
        return new ScheduledActivityResponse(
                activity.id().value(),
                activity.name(),
                activity.zoneId(),
                Mappings.toDto(activity.timeWindow()),
                Mappings.toDto(activity.scheduledPeriod()),
                activity.dependencyIds().stream().map(ActivityId::value).toList(),
                activity.estimatedDuration().toMinutes(),
                activity.estimatedRisk(),
                activity.requiredStaff().stream().map(ActivityMapper::toDto).toList(),
                activity.assignedStaffIds().stream().map(ResourceId::value).toList(),
                activity.requiredEquipment().stream().map(ActivityMapper::toDto).toList(),
                activity.assignedEquipmentIds().stream().map(ResourceId::value).toList(),
                activity.requiredSupplies().stream().map(ActivityMapper::toDto).toList(),
                activity.assignedSupplyIds().stream().map(ResourceId::value).toList(),
                Mappings.sorted(activity.requiredCertifications().stream().map(Certification::name)),
                Mappings.sorted(activity.requiredPermits().stream().map(Permit::name)),
                toResponse(activity.tracking()));
    }

    private static TrackingResponse toResponse(ActivityTrackingView tracking) {
        return new TrackingResponse(
                tracking.status(),
                tracking.startedAt().orElse(null),
                tracking.finishedAt().orElse(null),
                tracking.result().orElse(null),
                tracking.observations().stream()
                        .map(observation -> new ObservationDto(observation.note(), observation.recordedAt()))
                        .toList());
    }

    static StaffRequirementDto toDto(StaffRequirement requirement) {
        return new StaffRequirementDto(
                requirement.headcount(),
                Mappings.sorted(requirement.certifications().stream().map(Certification::name)));
    }

    static EquipmentRequirementDto toDto(ReusableRequirement requirement) {
        return new EquipmentRequirementDto(requirement.category().name(), requirement.count());
    }

    static SupplyRequirementDto toDto(DepletableRequirement requirement) {
        return new SupplyRequirementDto(requirement.category().name(), Mappings.toDto(requirement.quantity()));
    }
}
