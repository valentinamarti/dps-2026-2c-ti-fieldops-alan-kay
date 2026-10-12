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

import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.LITERS;

public class TransferRules implements ActivityRules {

    public static final ActivityType TYPE = new SingleParameterActivityType("Transfer", "distanceKilometers", TransferRules::new);

    private static final Duration LOADING_TIME = Duration.ofMinutes(30);
    private static final Duration TIME_PER_KILOMETER = Duration.ofMinutes(2);
    private static final int LONG_HAUL_THRESHOLD_KILOMETERS = 100;
    private static final BigDecimal FUEL_LITERS_PER_KILOMETER = new BigDecimal("0.15");

    private static final ResourceCategory FUEL = new ResourceCategory("Fuel");
    private static final ReusableRequirement ONE_TRUCK = new ReusableRequirement(new ResourceCategory("Truck"), 1);
    private static final StaffRequirement ONE_DRIVER = new StaffRequirement(1, Set.of(new Certification("Off-road driving")));

    private final int distanceKilometers;
    private final List<DepletableRequirement> supplies;

    public TransferRules(int distanceKilometers) {
        if (distanceKilometers <= 0) {
            throw new IllegalArgumentException("distance must be positive");
        }
        this.distanceKilometers = distanceKilometers;
        this.supplies = List.of(new DepletableRequirement(FUEL,
                new Quantity(FUEL_LITERS_PER_KILOMETER.multiply(BigDecimal.valueOf(distanceKilometers)), LITERS)));
    }

    @Override
    public Duration estimatedDuration() {
        return LOADING_TIME.plus(TIME_PER_KILOMETER.multipliedBy(distanceKilometers));
    }

    @Override
    public RiskLevel estimatedRisk() {
        return isLongHaul() ? RiskLevel.MEDIUM : RiskLevel.LOW;
    }

    @Override
    public List<ReusableRequirement> requiredEquipment() {
        return List.of(ONE_TRUCK);
    }

    @Override
    public List<DepletableRequirement> requiredSupplies() {
        return supplies;
    }

    @Override
    public List<StaffRequirement> requiredStaff() {
        return List.of(ONE_DRIVER);
    }

    private boolean isLongHaul() {
        return distanceKilometers > LONG_HAUL_THRESHOLD_KILOMETERS;
    }
}
