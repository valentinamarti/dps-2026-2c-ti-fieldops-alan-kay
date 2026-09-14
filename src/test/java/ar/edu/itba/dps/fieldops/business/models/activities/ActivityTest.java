package ar.edu.itba.dps.fieldops.business.models.activities;

import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.LITERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActivityTest {

    private static final TimePeriod WINDOW = new TimePeriod(
            LocalDateTime.of(2026, 10, 1, 8, 0), LocalDateTime.of(2026, 10, 1, 18, 0));
    private static final Zone ZONE = new Zone("z-1", "Coast", Set.of());
    private static final ActivityRules FIXED_RULES = new FixedRules();

    private static Activity activityDependingOn(List<Activity> dependencies) {
        return new Activity("a-1", "Survey", WINDOW, ZONE, dependencies, FIXED_RULES);
    }

    @Test
    void delegatesDurationEstimationToItsRules() {
        assertEquals(FIXED_RULES.estimatedDuration(), activityDependingOn(List.of()).estimatedDuration());
    }

    @Test
    void delegatesRiskEstimationToItsRules() {
        assertEquals(FIXED_RULES.estimatedRisk(), activityDependingOn(List.of()).estimatedRisk());
    }

    @Test
    void delegatesRequirementsToItsRules() {
        final var activity = activityDependingOn(List.of());

        assertEquals(FIXED_RULES.requiredEquipment(), activity.requiredEquipment());
        assertEquals(FIXED_RULES.requiredSupplies(), activity.requiredSupplies());
        assertEquals(FIXED_RULES.requiredStaff(), activity.requiredStaff());
    }

    @Test
    void dependsOnTheActivitiesItWasGiven() {
        final var landing = activityDependingOn(List.of());
        final var survey = activityDependingOn(List.of(landing));

        assertTrue(survey.dependsOn(landing));
    }

    @Test
    void doesNotDependOnActivitiesItWasNotGiven() {
        final var landing = activityDependingOn(List.of());
        final var survey = activityDependingOn(List.of());

        assertFalse(survey.dependsOn(landing));
    }

    /**
     * Stub with fixed values, so these tests do not depend on any concrete activity type.
     */
    private static class FixedRules implements ActivityRules {

        @Override
        public Duration estimatedDuration() {
            return Duration.ofHours(3);
        }

        @Override
        public RiskLevel estimatedRisk() {
            return RiskLevel.HIGH;
        }

        @Override
        public List<ReusableRequirement> requiredEquipment() {
            return List.of(new ReusableRequirement(new ResourceCategory("Boat"), 1));
        }

        @Override
        public List<DepletableRequirement> requiredSupplies() {
            return List.of(new DepletableRequirement(new ResourceCategory("Fuel"), Quantity.of("10", LITERS)));
        }

        @Override
        public List<StaffRequirement> requiredStaff() {
            return List.of(new StaffRequirement(2, Set.of()));
        }
    }
}
