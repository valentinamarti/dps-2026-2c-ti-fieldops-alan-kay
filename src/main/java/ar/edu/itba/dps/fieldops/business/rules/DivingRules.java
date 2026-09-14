package ar.edu.itba.dps.fieldops.business.rules;

import ar.edu.itba.dps.fieldops.business.models.activities.ActivityRules;
import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.LITERS;

public class DivingRules implements ActivityRules {

    private static final Duration BRIEFING_TIME = Duration.ofMinutes(45);
    private static final Duration TIME_PER_METER = Duration.ofMinutes(2);
    private static final int DEEP_DIVE_THRESHOLD_METERS = 30;
    private static final int DIVERS_PER_DIVE = 2;

    private static final Certification DIVING = new Certification("Diving");
    private static final StaffRequirement BUDDY_PAIR_OF_DIVERS = new StaffRequirement(DIVERS_PER_DIVE, Set.of(DIVING));
    private static final List<ReusableRequirement> EQUIPMENT = List.of(
            new ReusableRequirement(new ResourceCategory("Boat"), 1),
            new ReusableRequirement(new ResourceCategory("Oxygen tank"), DIVERS_PER_DIVE)
    );
    private static final List<DepletableRequirement> SUPPLIES = List.of(
            new DepletableRequirement(new ResourceCategory("Fuel"), Quantity.of("20", LITERS))
    );

    private final int maxDepthMeters;

    public DivingRules(int maxDepthMeters) {
        if (maxDepthMeters <= 0) {
            throw new IllegalArgumentException("max depth must be positive");
        }
        this.maxDepthMeters = maxDepthMeters;
    }

    @Override
    public Duration estimatedDuration() {
        return BRIEFING_TIME.plus(TIME_PER_METER.multipliedBy(maxDepthMeters));
    }

    @Override
    public RiskLevel estimatedRisk() {
        return isDeepDive() ? RiskLevel.HIGH : RiskLevel.MEDIUM;
    }

    @Override
    public List<ReusableRequirement> requiredEquipment() {
        return EQUIPMENT;
    }

    @Override
    public List<DepletableRequirement> requiredSupplies() {
        return SUPPLIES;
    }

    @Override
    public List<StaffRequirement> requiredStaff() {
        return List.of(BUDDY_PAIR_OF_DIVERS);
    }

    private boolean isDeepDive() {
        return maxDepthMeters > DEEP_DIVE_THRESHOLD_METERS;
    }
}
