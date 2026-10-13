package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;

import java.util.List;

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
