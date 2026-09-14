package ar.edu.itba.dps.fieldops.business.models.activities;

import java.time.Duration;
import java.util.List;

public interface ActivityRules {

    Duration estimatedDuration();

    RiskLevel estimatedRisk();

    List<ReusableRequirement> requiredEquipment();

    List<DepletableRequirement> requiredSupplies();

    List<StaffRequirement> requiredStaff();
}
