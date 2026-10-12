package ar.edu.itba.dps.fieldops.business.models.activities;

import ar.edu.itba.dps.fieldops.business.interfaces.activities.ActivityRules;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.LITERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActivityTest {

    private static final TimePeriod WINDOW = new TimePeriod(
            LocalDateTime.of(2026, 10, 1, 8, 0), LocalDateTime.of(2026, 10, 1, 18, 0));
    private static final Zone ZONE = new Zone("z-1", "Coast", Set.of());
    private static final ActivityRules FIXED_RULES = new FixedRules();
    private static final Certification FIRST_AID = new Certification("First aid");
    private static final Permit SURVEY_PERMIT_OF_THE_ZONE = new Permit("Survey in z-1");

    private static Activity activityDependingOn(List<Activity> dependencies) {
        return new Activity(new ActivityId("a-1"), "Survey", WINDOW, ZONE, dependencies, FIXED_RULES);
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

    @Test
    void rejectsABlankOrMissingIdentity() {
        assertThrows(IllegalArgumentException.class,
                () -> new Activity(new ActivityId(""), "Survey", WINDOW, ZONE, List.of(), FIXED_RULES));
        assertThrows(IllegalArgumentException.class,
                () -> new Activity(new ActivityId("a-1"), " ", WINDOW, ZONE, List.of(), FIXED_RULES));
        assertThrows(NullPointerException.class,
                () -> new Activity(null, "Survey", WINDOW, ZONE, List.of(), FIXED_RULES));
    }

    @Test
    void rejectsMissingCollaborators() {
        assertThrows(NullPointerException.class,
                () -> new Activity(new ActivityId("a-1"), "Survey", null, ZONE, List.of(), FIXED_RULES));
        assertThrows(NullPointerException.class,
                () -> new Activity(new ActivityId("a-1"), "Survey", WINDOW, null, List.of(), FIXED_RULES));
        assertThrows(NullPointerException.class,
                () -> new Activity(new ActivityId("a-1"), "Survey", WINDOW, ZONE, null, FIXED_RULES));
        assertThrows(NullPointerException.class,
                () -> new Activity(new ActivityId("a-1"), "Survey", WINDOW, ZONE, List.of(), null));
    }

    @Test
    void dependenciesCannotBeChangedFromOutside() {
        final var survey = activityDependingOn(List.of());

        assertThrows(UnsupportedOperationException.class, () -> survey.getDependencies().add(survey));
    }

    @Test
    void asksNoPermitsNorCertificationsOfAllItsStaffUnlessItsRulesDo() {
        final var activity = activityDependingOn(List.of());

        assertTrue(activity.requiredPermits().isEmpty());
        assertTrue(activity.missingPermits(List.of()).isEmpty());
        assertTrue(activity.requiredCertificationsForAllStaff().isEmpty());
        assertTrue(activity.admits(_ -> false));
    }

    @Test
    void asksItsRulesForThePermitsOfItsOwnZone() {
        final var activity = activityWithDemandingRules();

        assertEquals(Set.of(SURVEY_PERMIT_OF_THE_ZONE), activity.requiredPermits());
    }

    @Test
    void reportsThePermitsItRequiresThatWereNotGranted() {
        final var activity = activityWithDemandingRules();

        assertEquals(Set.of(SURVEY_PERMIT_OF_THE_ZONE), activity.missingPermits(List.of(new Permit("Survey in z-2"))));
        assertTrue(activity.missingPermits(List.of(SURVEY_PERMIT_OF_THE_ZONE)).isEmpty());
    }

    @Test
    void admitsOnlyWhoHoldsEveryCertificationItsRulesRequireOfAllTheStaff() {
        final var activity = activityWithDemandingRules();

        assertEquals(Set.of(FIRST_AID), activity.requiredCertificationsForAllStaff());
        assertTrue(activity.admits(FIRST_AID::equals));
        assertFalse(activity.admits(_ -> false));
    }

    private static Activity activityWithDemandingRules() {
        return new Activity(new ActivityId("a-1"), "Survey", WINDOW, ZONE, List.of(), new DemandingRules());
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

    private static class DemandingRules extends FixedRules {

        @Override
        public Set<Certification> requiredCertificationsForAllStaff() {
            return Set.of(FIRST_AID);
        }

        @Override
        public Set<Permit> requiredPermitsIn(Zone zone) {
            return Set.of(new Permit("Survey in " + zone.getId()));
        }
    }
}
