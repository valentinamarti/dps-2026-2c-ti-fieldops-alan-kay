package ar.edu.itba.dps.fieldops.business.rules;

import ar.edu.itba.dps.fieldops.business.interfaces.activities.ActivityRules;
import ar.edu.itba.dps.fieldops.business.interfaces.activities.ActivityType;
import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;
import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;

import java.time.Duration;
import java.util.List;
import java.util.Set;

public class DroneSurveyRules implements ActivityRules {

    public static final ActivityType TYPE = new SingleParameterActivityType("Drone survey", "areaHectares", DroneSurveyRules::new);

    private static final Duration SETUP_TIME = Duration.ofMinutes(30);
    private static final Duration TIME_PER_HECTARE = Duration.ofMinutes(3);
    private static final int LINE_OF_SIGHT_LIMIT_HECTARES = 50;

    private static final StaffRequirement ONE_PILOT = new StaffRequirement(1, Set.of(new Certification("Drone pilot")));
    private static final ReusableRequirement ONE_DRONE = new ReusableRequirement(new ResourceCategory("Drone"), 1);

    private final int areaHectares;

    public DroneSurveyRules(int areaHectares) {
        if (areaHectares <= 0) {
            throw new IllegalArgumentException("area must be positive");
        }
        this.areaHectares = areaHectares;
    }

    @Override
    public Duration estimatedDuration() {
        return SETUP_TIME.plus(TIME_PER_HECTARE.multipliedBy(areaHectares));
    }

    @Override
    public RiskLevel estimatedRisk() {
        return fliesBeyondLineOfSight() ? RiskLevel.MEDIUM : RiskLevel.LOW;
    }

    @Override
    public List<ReusableRequirement> requiredEquipment() {
        return List.of(ONE_DRONE);
    }

    @Override
    public List<DepletableRequirement> requiredSupplies() {
        return List.of();
    }

    @Override
    public List<StaffRequirement> requiredStaff() {
        return List.of(ONE_PILOT);
    }

    private boolean fliesBeyondLineOfSight() {
        return areaHectares > LINE_OF_SIGHT_LIMIT_HECTARES;
    }
}
