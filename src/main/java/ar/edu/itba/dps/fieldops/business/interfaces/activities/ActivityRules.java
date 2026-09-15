package ar.edu.itba.dps.fieldops.business.interfaces.activities;

import java.time.Duration;
import java.util.List;

import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;

public interface ActivityRules {

    Duration estimatedDuration();

    RiskLevel estimatedRisk();

    List<ReusableRequirement> requiredEquipment();

    List<DepletableRequirement> requiredSupplies();

    List<StaffRequirement> requiredStaff();
}
