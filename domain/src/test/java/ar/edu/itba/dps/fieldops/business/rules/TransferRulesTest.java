package ar.edu.itba.dps.fieldops.business.rules;

import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.LITERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransferRulesTest {

    @Test
    void durationIsLoadingTimePlusTwoMinutesPerKilometer() {
        assertEquals(Duration.ofMinutes(150), new TransferRules(60).estimatedDuration());
    }

    @Test
    void transferUpToAHundredKilometersIsLowRisk() {
        assertEquals(RiskLevel.LOW, new TransferRules(100).estimatedRisk());
    }

    @Test
    void transferLongerThanAHundredKilometersIsMediumRisk() {
        assertEquals(RiskLevel.MEDIUM, new TransferRules(101).estimatedRisk());
    }

    @Test
    void requiresACertifiedOffRoadDriver() {
        final var driver = new StaffRequirement(1, Set.of(new Certification("Off-road driving")));

        assertEquals(List.of(driver), new TransferRules(60).requiredStaff());
    }

    @Test
    void requiresATruck() {
        final var truck = new ReusableRequirement(new ResourceCategory("Truck"), 1);

        assertEquals(List.of(truck), new TransferRules(60).requiredEquipment());
    }

    @Test
    void consumesFuelInProportionToTheDistance() {
        final var fuel = new DepletableRequirement(new ResourceCategory("Fuel"), Quantity.of("9", LITERS));

        assertEquals(List.of(fuel), new TransferRules(60).requiredSupplies());
    }

    @Test
    void rejectsNonPositiveDistance() {
        assertThrows(IllegalArgumentException.class, () -> new TransferRules(0));
    }
}
