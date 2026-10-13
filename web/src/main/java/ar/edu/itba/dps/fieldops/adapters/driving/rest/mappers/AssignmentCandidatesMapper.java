package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.AssignmentCandidatesResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.EquipmentCandidatesDto;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.StaffCandidatesDto;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.SupplyCandidatesDto;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.DepletableResource;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.Equipment;
import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;
import ar.edu.itba.dps.fieldops.business.models.resources.Depletable;
import ar.edu.itba.dps.fieldops.business.models.resources.Instrument;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.Vehicle;
import ar.edu.itba.dps.fieldops.business.usecases.AssignmentCandidates;

public final class AssignmentCandidatesMapper {
    private AssignmentCandidatesMapper() {
    }

    public static AssignmentCandidatesResponse toResponse(AssignmentCandidates candidates) {
        return new AssignmentCandidatesResponse(
                candidates.staff().stream().map(AssignmentCandidatesMapper::toStaffCandidates).toList(),
                candidates.equipment().stream().map(AssignmentCandidatesMapper::toEquipmentCandidates).toList(),
                candidates.supplies().stream().map(AssignmentCandidatesMapper::toSupplyCandidates).toList());
    }

    private static StaffCandidatesDto toStaffCandidates(AssignmentCandidates.For<StaffRequirement, Person> entry) {
        return new StaffCandidatesDto(ActivityMapper.toDto(entry.requirement()),
                entry.candidates().stream().map(StaffMapper::toResponse).toList());
    }

    private static EquipmentCandidatesDto toEquipmentCandidates(
            AssignmentCandidates.For<ReusableRequirement, Equipment> entry) {
        return new EquipmentCandidatesDto(ActivityMapper.toDto(entry.requirement()),
                entry.candidates().stream()
                        .filter(Vehicle.class::isInstance).map(Vehicle.class::cast)
                        .map(VehicleMapper::toResponse).toList(),
                entry.candidates().stream()
                        .filter(Instrument.class::isInstance).map(Instrument.class::cast)
                        .map(InstrumentMapper::toResponse).toList());
    }

    private static SupplyCandidatesDto toSupplyCandidates(
            AssignmentCandidates.For<DepletableRequirement, DepletableResource> entry) {
        return new SupplyCandidatesDto(ActivityMapper.toDto(entry.requirement()),
                entry.candidates().stream().map(Depletable.class::cast).map(SupplyMapper::toResponse).toList());
    }
}
