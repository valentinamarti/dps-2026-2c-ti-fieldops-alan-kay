package ar.edu.itba.dps.fieldops.business.models.activities;

import ar.edu.itba.dps.fieldops.business.interfaces.activities.ActivityRules;
import ar.edu.itba.dps.fieldops.business.models.common.DomainArguments;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import lombok.Getter;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public class Activity {

    @Getter
    private final String id;
    @Getter
    private final String name;
    @Getter
    private final TimePeriod timeWindow;
    @Getter
    private final Zone zone;
    @Getter
    private final List<Activity> dependencies;
    private final ActivityRules rules;

    public Activity(String id, String name, TimePeriod timeWindow, Zone zone, List<Activity> dependencies, ActivityRules rules) {
        this.id = DomainArguments.requireText(id, "id");
        this.name = DomainArguments.requireText(name, "name");
        this.timeWindow = Objects.requireNonNull(timeWindow, "timeWindow is required");
        this.zone = Objects.requireNonNull(zone, "zone is required");
        this.dependencies = DomainArguments.requireList(dependencies, "dependencies");
        this.rules = Objects.requireNonNull(rules, "rules are required");
    }

    public boolean dependsOn(Activity other) {
        return dependencies.contains(other);
    }

    public Duration estimatedDuration() {
        return rules.estimatedDuration();
    }

    public RiskLevel estimatedRisk() {
        return rules.estimatedRisk();
    }

    public List<ReusableRequirement> requiredEquipment() {
        return rules.requiredEquipment();
    }

    public List<DepletableRequirement> requiredSupplies() {
        return rules.requiredSupplies();
    }

    public List<StaffRequirement> requiredStaff() {
        return rules.requiredStaff();
    }
}
