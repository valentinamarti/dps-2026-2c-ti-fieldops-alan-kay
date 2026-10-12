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

/**
 * Translates {@link AssignmentCandidates} to its DTO (D56). It only reads: nothing here becomes a command,
 * because candidates are not assigned by naming them, only by {@code POST .../staff}, {@code .../equipment}
 * or {@code .../supplies} with a resource id.
 */
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

    /**
     * A candidate that qualifies can be either kind of equipment, so it is split into the same two lists
     * {@code GET /api/equipment} already uses (D52): a vehicle and an instrument do not have the same shape.
     */
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
