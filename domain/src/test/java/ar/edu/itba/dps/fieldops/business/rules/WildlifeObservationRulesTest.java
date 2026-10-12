package ar.edu.itba.dps.fieldops.business.rules;

import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WildlifeObservationRulesTest {

    @Test
    void durationIsApproachTimePlusFortyFiveMinutesPerObservationPoint() {
        assertEquals(Duration.ofMinutes(120), new WildlifeObservationRules(2).estimatedDuration());
    }

    @Test
    void riskIsLow() {
        assertEquals(RiskLevel.LOW, new WildlifeObservationRules(2).estimatedRisk());
    }

    @Test
    void requiresAPairOfObserversWithoutSpecificCertifications() {
        assertEquals(List.of(new StaffRequirement(2, Set.of())), new WildlifeObservationRules(2).requiredStaff());
    }

    @Test
    void requiresBinocularsForEachObserver() {
        final var binoculars = new ReusableRequirement(new ResourceCategory("Binoculars"), 2);

        assertEquals(List.of(binoculars), new WildlifeObservationRules(2).requiredEquipment());
    }

    @Test
    void consumesNoSupplies() {
        assertTrue(new WildlifeObservationRules(2).requiredSupplies().isEmpty());
    }

    @Test
    void rejectsNonPositiveObservationPoints() {
        assertThrows(IllegalArgumentException.class, () -> new WildlifeObservationRules(0));
    }
}
