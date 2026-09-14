package ar.edu.itba.dps.fieldops.business.models.activities;

import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public class Activity {

    private final String id;
    private final String name;
    private final TimePeriod timeWindow;
    private final Zone zone;
    private final List<Activity> dependencies;
    private final ActivityRules rules;

    public Activity(String id, String name, TimePeriod timeWindow, Zone zone, List<Activity> dependencies, ActivityRules rules) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.name = Objects.requireNonNull(name, "name is required");
        this.timeWindow = Objects.requireNonNull(timeWindow, "time window is required");
        this.zone = Objects.requireNonNull(zone, "zone is required");
        this.dependencies = List.copyOf(dependencies);
        this.rules = Objects.requireNonNull(rules, "rules are required");
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public TimePeriod timeWindow() {
        return timeWindow;
    }

    public Zone zone() {
        return zone;
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
