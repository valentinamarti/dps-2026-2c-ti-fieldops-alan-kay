package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;

import java.util.List;

/**
 * An activity as it was scheduled, with what its rules demand of it and what was assigned to it so far. The
 * requirements are part of the answer because they are the only honest way of showing what the type and the
 * night variant added: a wrapped rule does not say that it is nocturnal (D42), and they are also what the
 * client needs in order to assign resources next (D53). The assigned ids are the only way to observe an
 * assignment over HTTP, because {@code POST .../staff}, {@code .../equipment} and {@code .../supplies} answer
 * 204 and there is no {@code GET} of an assignment on its own (D56).
 *
 * @param estimatedDurationMinutes the estimated duration in minutes, so the unit is in the name of the field
 * @param requiredCertifications   the certifications demanded of everyone assigned, not of a single post
 * @param requiredPermits          the permits the activity demands in its zone, with the name the expedition
 *                                 has to grant (D43)
 */
public record ScheduledActivityResponse(String id, String name, String zoneId, PeriodDto timeWindow,
                                        PeriodDto scheduledPeriod, List<String> dependencyIds,
                                        long estimatedDurationMinutes, RiskLevel estimatedRisk,
                                        List<StaffRequirementDto> requiredStaff, List<String> assignedStaffIds,
                                        List<EquipmentRequirementDto> requiredEquipment,
                                        List<String> assignedEquipmentIds,
                                        List<SupplyRequirementDto> requiredSupplies,
                                        List<String> assignedSupplyIds,
                                        List<String> requiredCertifications, List<String> requiredPermits) {
}
