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

class DivingRulesTest {

    @Test
    void durationIsBriefingTimePlusTwoMinutesPerMeterOfDepth() {
        assertEquals(Duration.ofMinutes(65), new DivingRules(10).estimatedDuration());
    }

    @Test
    void diveUpToThirtyMetersIsMediumRisk() {
        assertEquals(RiskLevel.MEDIUM, new DivingRules(30).estimatedRisk());
    }

    @Test
    void diveDeeperThanThirtyMetersIsHighRisk() {
        assertEquals(RiskLevel.HIGH, new DivingRules(31).estimatedRisk());
    }

    @Test
    void requiresAPairOfCertifiedDivers() {
        final var certifiedPair = new StaffRequirement(2, Set.of(new Certification("Diving")));

        assertEquals(List.of(certifiedPair), new DivingRules(10).requiredStaff());
    }

    @Test
    void requiresABoatAndOneOxygenTankPerDiver() {
        final var expected = List.of(
                new ReusableRequirement(new ResourceCategory("Boat"), 1),
                new ReusableRequirement(new ResourceCategory("Oxygen tank"), 2)
        );

        assertEquals(expected, new DivingRules(10).requiredEquipment());
    }

    @Test
    void consumesFuelForTheBoat() {
        final var fuel = new DepletableRequirement(new ResourceCategory("Fuel"), Quantity.of("20", LITERS));

        assertEquals(List.of(fuel), new DivingRules(10).requiredSupplies());
    }

    @Test
    void rejectsNonPositiveDepth() {
        assertThrows(IllegalArgumentException.class, () -> new DivingRules(0));
    }
}
