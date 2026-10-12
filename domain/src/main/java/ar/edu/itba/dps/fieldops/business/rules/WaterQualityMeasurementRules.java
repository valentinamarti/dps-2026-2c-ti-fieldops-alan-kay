package ar.edu.itba.dps.fieldops.business.rules;

import ar.edu.itba.dps.fieldops.business.interfaces.activities.ActivityRules;
import ar.edu.itba.dps.fieldops.business.interfaces.activities.ActivityType;
import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;

public class WaterQualityMeasurementRules implements ActivityRules {

    public static final ActivityType TYPE = new SingleParameterActivityType("Water quality measurement", "stationCount", WaterQualityMeasurementRules::new);

    private static final Duration CALIBRATION_TIME = Duration.ofMinutes(30);
    private static final Duration TIME_PER_STATION = Duration.ofMinutes(15);
    private static final ResourceCategory REAGENT_KIT = new ResourceCategory("Reagent kit");
    private static final ReusableRequirement ONE_PROBE = new ReusableRequirement(new ResourceCategory("Multiparameter probe"), 1);
    private static final StaffRequirement ONE_ANALYST = new StaffRequirement(1, Set.of(new Certification("Water analysis")));

    private final int stationCount;
    private final List<DepletableRequirement> supplies;

    public WaterQualityMeasurementRules(int stationCount) {
        if (stationCount <= 0) {
            throw new IllegalArgumentException("station count must be positive");
        }
        this.stationCount = stationCount;
        this.supplies = List.of(new DepletableRequirement(REAGENT_KIT, new Quantity(BigDecimal.valueOf(stationCount), UNITS)));
    }

    @Override
    public Duration estimatedDuration() {
        return CALIBRATION_TIME.plus(TIME_PER_STATION.multipliedBy(stationCount));
    }

    @Override
    public RiskLevel estimatedRisk() {
        return RiskLevel.LOW;
    }

    @Override
    public List<ReusableRequirement> requiredEquipment() {
        return List.of(ONE_PROBE);
    }

    @Override
    public List<DepletableRequirement> requiredSupplies() {
        return supplies;
    }

    @Override
    public List<StaffRequirement> requiredStaff() {
        return List.of(ONE_ANALYST);
    }
}
