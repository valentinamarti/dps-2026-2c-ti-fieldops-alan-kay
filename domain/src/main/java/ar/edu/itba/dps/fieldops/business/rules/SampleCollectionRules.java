package ar.edu.itba.dps.fieldops.business.rules;

import ar.edu.itba.dps.fieldops.business.interfaces.activities.ActivityRules;
import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;

public class SampleCollectionRules implements ActivityRules {

    private static final Duration SETUP_TIME = Duration.ofHours(1);
    private static final Duration TIME_PER_SAMPLE = Duration.ofMinutes(20);
    private static final ResourceCategory SAMPLE_CONTAINER = new ResourceCategory("Sample container");
    private static final ReusableRequirement ONE_COOLER = new ReusableRequirement(new ResourceCategory("Cooler"), 1);
    private static final StaffRequirement ONE_FIELD_WORKER = new StaffRequirement(1, Set.of());

    private final int sampleCount;
    private final List<DepletableRequirement> supplies;

    public SampleCollectionRules(int sampleCount) {
        if (sampleCount <= 0) {
            throw new IllegalArgumentException("sample count must be positive");
        }
        this.sampleCount = sampleCount;
        this.supplies = List.of(new DepletableRequirement(SAMPLE_CONTAINER, new Quantity(BigDecimal.valueOf(sampleCount), UNITS)));
    }

    @Override
    public Duration estimatedDuration() {
        return SETUP_TIME.plus(TIME_PER_SAMPLE.multipliedBy(sampleCount));
    }

    @Override
    public RiskLevel estimatedRisk() {
        return RiskLevel.LOW;
    }

    @Override
    public List<ReusableRequirement> requiredEquipment() {
        return List.of(ONE_COOLER);
    }

    @Override
    public List<DepletableRequirement> requiredSupplies() {
        return supplies;
    }

    @Override
    public List<StaffRequirement> requiredStaff() {
        return List.of(ONE_FIELD_WORKER);
    }
}
