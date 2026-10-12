package ar.edu.itba.dps.fieldops.business.rules;

import ar.edu.itba.dps.fieldops.business.interfaces.activities.ActivityRules;
import ar.edu.itba.dps.fieldops.business.interfaces.activities.ActivityType;
import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;

import java.time.Duration;
import java.util.List;
import java.util.Set;

public class WildlifeObservationRules implements ActivityRules {

    public static final ActivityType TYPE = new SingleParameterActivityType("Wildlife observation", "observationPoints", WildlifeObservationRules::new);

    private static final Duration APPROACH_TIME = Duration.ofMinutes(30);
    private static final Duration TIME_PER_POINT = Duration.ofMinutes(45);
    private static final int OBSERVERS_PER_OUTING = 2;

    private static final StaffRequirement PAIR_OF_OBSERVERS = new StaffRequirement(OBSERVERS_PER_OUTING, Set.of());
    private static final ReusableRequirement BINOCULARS_PER_OBSERVER = new ReusableRequirement(new ResourceCategory("Binoculars"), OBSERVERS_PER_OUTING);

    private final int observationPoints;

    public WildlifeObservationRules(int observationPoints) {
        if (observationPoints <= 0) {
            throw new IllegalArgumentException("observation points must be positive");
        }
        this.observationPoints = observationPoints;
    }

    @Override
    public Duration estimatedDuration() {
        return APPROACH_TIME.plus(TIME_PER_POINT.multipliedBy(observationPoints));
    }

    @Override
    public RiskLevel estimatedRisk() {
        return RiskLevel.LOW;
    }

    @Override
    public List<ReusableRequirement> requiredEquipment() {
        return List.of(BINOCULARS_PER_OBSERVER);
    }

    @Override
    public List<DepletableRequirement> requiredSupplies() {
        return List.of();
    }

    @Override
    public List<StaffRequirement> requiredStaff() {
        return List.of(PAIR_OF_OBSERVERS);
    }
}
