package ar.edu.itba.dps.fieldops.business.interfaces.activities;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;
import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;

public interface ActivityRules {

    Duration estimatedDuration();

    RiskLevel estimatedRisk();

    List<ReusableRequirement> requiredEquipment();

    List<DepletableRequirement> requiredSupplies();

    List<StaffRequirement> requiredStaff();

    default Set<Certification> requiredCertificationsForAllStaff() {
        return Set.of();
    }

    default Set<Permit> requiredPermitsIn(Zone zone) {
        return Set.of();
    }
}
