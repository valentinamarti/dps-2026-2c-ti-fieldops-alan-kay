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

/**
 * Translates between the activity DTOs and the domain, in both directions (D49). The request never becomes an
 * {@code Activity} here: it becomes a command with ids, a type name and its parameters, and the use case is
 * the one that resolves them (D53).
 *
 * <p>It does not validate: a field that is missing travels as {@code null} and the domain rejects it with its
 * own message, which the error handler turns into a 400.
 */
public final class ActivityMapper {

    private ActivityMapper() {
    }

    public static ScheduleActivityCommand toCommand(String expeditionId, ScheduleActivityRequest request) {
        return new ScheduleActivityCommand(new ExpeditionId(expeditionId), toPlannedActivity(request));
    }

    /**
     * The description of one activity, which is the same whether it is scheduled on its own or as a leaf of a
     * block, so the mapper of the itinerary reuses this one (D54).
     */
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
                // In the order the activity keeps them, which is the one they were sent in, and not sorted.
                activity.getDependencies().stream().map(dependency -> dependency.getId().value()).toList(),
                activity.estimatedDuration().toMinutes(),
                activity.estimatedRisk(),
                activity.requiredStaff().stream().map(ActivityMapper::toDto).toList(),
                activity.requiredEquipment().stream().map(ActivityMapper::toDto).toList(),
                activity.requiredSupplies().stream().map(ActivityMapper::toDto).toList(),
                Mappings.sorted(activity.requiredCertificationsForAllStaff().stream().map(Certification::name)),
                Mappings.sorted(activity.requiredPermits().stream().map(Permit::name)));
    }

    private static StaffRequirementDto toDto(StaffRequirement requirement) {
        return new StaffRequirementDto(
                requirement.headcount(),
                Mappings.sorted(requirement.certifications().stream().map(Certification::name)));
    }

    private static EquipmentRequirementDto toDto(ReusableRequirement requirement) {
        return new EquipmentRequirementDto(requirement.category().name(), requirement.count());
    }

    private static SupplyRequirementDto toDto(DepletableRequirement requirement) {
        return new SupplyRequirementDto(requirement.category().name(), Mappings.toDto(requirement.quantity()));
    }
}
