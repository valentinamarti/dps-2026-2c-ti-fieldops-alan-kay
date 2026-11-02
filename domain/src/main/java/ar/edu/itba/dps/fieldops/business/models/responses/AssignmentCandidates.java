package ar.edu.itba.dps.fieldops.business.models.responses;

import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;

import java.util.List;

public record AssignmentCandidates(List<ForStaff> staff, List<ForEquipment> equipment, List<ForSupplies> supplies) {

    public record ForStaff(StaffRequirement requirement, List<PersonView> candidates) {
    }

    public record ForEquipment(ReusableRequirement requirement, List<VehicleView> vehicles, List<InstrumentView> instruments) {
    }

    public record ForSupplies(DepletableRequirement requirement, List<SupplyView> candidates) {
    }
}
