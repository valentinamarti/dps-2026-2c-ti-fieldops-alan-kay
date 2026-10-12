package ar.edu.itba.dps.fieldops.business.rules;

import ar.edu.itba.dps.fieldops.business.interfaces.activities.ActivityRules;
import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;
import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class ActivityRulesDecorator implements ActivityRules {

    private final ActivityRules base;

    protected ActivityRulesDecorator(ActivityRules base) {
        this.base = Objects.requireNonNull(base, "base rules are required");
    }

    @Override
    public Duration estimatedDuration() {
        return base.estimatedDuration();
    }

    @Override
    public RiskLevel estimatedRisk() {
        return base.estimatedRisk();
    }

    @Override
    public List<ReusableRequirement> requiredEquipment() {
        return base.requiredEquipment();
    }

    @Override
    public List<DepletableRequirement> requiredSupplies() {
        return base.requiredSupplies();
    }

    @Override
    public List<StaffRequirement> requiredStaff() {
        return base.requiredStaff();
    }

    @Override
    public Set<Certification> requiredCertificationsForAllStaff() {
        return base.requiredCertificationsForAllStaff();
    }

    @Override
    public Set<Permit> requiredPermitsIn(Zone zone) {
        return base.requiredPermitsIn(zone);
    }
}
