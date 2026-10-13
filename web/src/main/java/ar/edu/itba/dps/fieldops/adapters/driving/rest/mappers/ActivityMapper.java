package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.EquipmentRequirementDto;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ScheduleActivityRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ScheduledActivityResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.StaffRequirementDto;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.SupplyRequirementDto;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.usecases.PlannedActivityCommand;
import ar.edu.itba.dps.fieldops.business.usecases.ScheduleActivityCommand;

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

    public static ScheduledActivityResponse toResponse(ItineraryItem item) {
        final var activity = item.getActivity();
        return new ScheduledActivityResponse(
                activity.getId().value(),
                activity.getName(),
                activity.getZone().getId(),
                Mappings.toDto(activity.getTimeWindow()),
                Mappings.toDto(item.getScheduledPeriod()),

                activity.getDependencies().stream().map(dependency -> dependency.getId().value()).toList(),
                activity.estimatedDuration().toMinutes(),
                activity.estimatedRisk(),
                activity.requiredStaff().stream().map(ActivityMapper::toDto).toList(),

                item.getAssignedStaff().stream().map(person -> person.getId().value()).toList(),
                activity.requiredEquipment().stream().map(ActivityMapper::toDto).toList(),
                item.getAssignedEquipment().stream().map(equipment -> equipment.getId().value()).toList(),
                activity.requiredSupplies().stream().map(ActivityMapper::toDto).toList(),
                item.getAssignedSupplies().stream().map(supply -> supply.getId().value()).toList(),
                Mappings.sorted(activity.requiredCertificationsForAllStaff().stream().map(Certification::name)),
                Mappings.sorted(activity.requiredPermits().stream().map(Permit::name)));
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
