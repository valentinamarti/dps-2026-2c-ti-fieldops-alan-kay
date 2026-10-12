package ar.edu.itba.dps.fieldops.business.rules;

import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;
import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DroneSurveyRulesTest {

    @Test
    void durationIsSetupTimePlusThreeMinutesPerHectare() {
        assertEquals(Duration.ofMinutes(90), new DroneSurveyRules(20).estimatedDuration());
    }

    @Test
    void surveyUpToFiftyHectaresIsLowRisk() {
        assertEquals(RiskLevel.LOW, new DroneSurveyRules(50).estimatedRisk());
    }

    @Test
    void surveyLargerThanFiftyHectaresIsMediumRisk() {
        assertEquals(RiskLevel.MEDIUM, new DroneSurveyRules(51).estimatedRisk());
    }

    @Test
    void requiresACertifiedDronePilot() {
        final var pilot = new StaffRequirement(1, Set.of(new Certification("Drone pilot")));

        assertEquals(List.of(pilot), new DroneSurveyRules(20).requiredStaff());
    }

    @Test
    void requiresADrone() {
        final var drone = new ReusableRequirement(new ResourceCategory("Drone"), 1);

        assertEquals(List.of(drone), new DroneSurveyRules(20).requiredEquipment());
    }

    @Test
    void consumesNoSupplies() {
        assertTrue(new DroneSurveyRules(20).requiredSupplies().isEmpty());
    }

    @Test
    void rejectsNonPositiveArea() {
        assertThrows(IllegalArgumentException.class, () -> new DroneSurveyRules(0));
    }
}
