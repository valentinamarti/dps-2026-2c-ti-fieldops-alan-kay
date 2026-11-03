package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.AssignmentCandidatesResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.EquipmentCandidatesDto;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.StaffCandidatesDto;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.SupplyCandidatesDto;
import ar.edu.itba.dps.fieldops.business.models.responses.AssignmentCandidates;

public final class AssignmentCandidatesMapper {
    private AssignmentCandidatesMapper() {
    }

    public static AssignmentCandidatesResponse toResponse(AssignmentCandidates candidates) {
        return new AssignmentCandidatesResponse(
                candidates.staff().stream().map(AssignmentCandidatesMapper::toStaffCandidates).toList(),
                candidates.equipment().stream().map(AssignmentCandidatesMapper::toEquipmentCandidates).toList(),
                candidates.supplies().stream().map(AssignmentCandidatesMapper::toSupplyCandidates).toList());
    }

    private static StaffCandidatesDto toStaffCandidates(AssignmentCandidates.ForStaff entry) {
        return new StaffCandidatesDto(ActivityMapper.toDto(entry.requirement()),
                entry.candidates().stream().map(StaffMapper::toResponse).toList());
    }

    private static EquipmentCandidatesDto toEquipmentCandidates(AssignmentCandidates.ForEquipment entry) {
        return new EquipmentCandidatesDto(ActivityMapper.toDto(entry.requirement()),
                entry.vehicles().stream().map(VehicleMapper::toResponse).toList(),
                entry.instruments().stream().map(InstrumentMapper::toResponse).toList());
    }

    private static SupplyCandidatesDto toSupplyCandidates(AssignmentCandidates.ForSupplies entry) {
        return new SupplyCandidatesDto(ActivityMapper.toDto(entry.requirement()),
                entry.candidates().stream().map(SupplyMapper::toResponse).toList());
    }
}
