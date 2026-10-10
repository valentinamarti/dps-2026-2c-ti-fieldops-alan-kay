package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.exceptions.ActivitiesInProgressException;
import ar.edu.itba.dps.fieldops.business.exceptions.InsufficientStockException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidStatusTransitionException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidTrackingException;
import ar.edu.itba.dps.fieldops.business.interfaces.activities.ActivityRules;
import ar.edu.itba.dps.fieldops.business.interfaces.validation.ExpeditionValidator;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.activities.DepletableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.activities.StaffRequirement;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.resources.Depletable;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.validation.ApprovalResult;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.supply;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.LITERS;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpeditionTrackingTest {

    private static final ExpeditionValidator WITHOUT_PROBLEMS = _ -> new ApprovalResult(List.of());

    private final Zone coast = zone("z-1");
    private final Expedition expedition = expeditionIn(coast, person("p-1")).build();
    private final Depletable jars = supply("d-1", "Sample container", Quantity.of("10", UNITS));
    private final ItineraryItem item = expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(9, 11));

    private static LocalDateTime at(int hour, int minute) {
        return LocalDateTime.of(2026, 10, 1, hour, minute);
    }

    private Expedition inExecution() {
        expedition.assignStaff(item, person("p-2"));
        expedition.assignSupply(item, jars);
        expedition.submitForReview();
        expedition.approve(WITHOUT_PROBLEMS, List.of());
        expedition.start();
        return expedition;
    }

    @Test
    void anApprovedExpeditionRunsGetsSuspendedResumesAndFinishes() {
        inExecution();
        assertEquals(ExpeditionStatus.IN_EXECUTION, expedition.getStatus());

        expedition.suspend();
        assertEquals(ExpeditionStatus.SUSPENDED, expedition.getStatus());

        expedition.resume();
        assertEquals(ExpeditionStatus.IN_EXECUTION, expedition.getStatus());

        expedition.finish();
        assertEquals(ExpeditionStatus.FINISHED, expedition.getStatus());
    }

    @Test
    void anExpeditionCannotFinishWithAnActivityInProgress() {
        inExecution().startActivity(item, at(9, 0));

        assertThrows(ActivitiesInProgressException.class, expedition::finish);
        assertEquals(ExpeditionStatus.IN_EXECUTION, expedition.getStatus());
    }

    @Test
    void aSuspendedExpeditionCannotFinishWithAnActivityInProgressEither() {
        inExecution().startActivity(item, at(9, 0));
        expedition.suspend();

        assertThrows(ActivitiesInProgressException.class, expedition::finish);
        assertEquals(ExpeditionStatus.SUSPENDED, expedition.getStatus());
    }

    @Test
    void anExpeditionFinishesOnceItsActivityInProgressIsClosed() {
        inExecution().startActivity(item, at(9, 0));
        expedition.finishActivity(item, at(11, 0), "Samples taken");

        expedition.finish();

        assertEquals(ExpeditionStatus.FINISHED, expedition.getStatus());
    }

    @Test
    void aFinishedExpeditionDoesNotGoAnywhereElse() {
        inExecution().finish();

        assertThrows(InvalidStatusTransitionException.class, expedition::resume);
    }

    @Test
    void recordsWhenAnActivityStartedAndFinishedWithItsResult() {
        inExecution().startActivity(item, at(9, 5));

        expedition.finishActivity(item, at(10, 50), "Twelve samples collected");

        final var tracking = item.getTracking().orElseThrow();
        assertEquals(at(9, 5), tracking.getStartedAt());
        assertEquals(at(10, 50), tracking.getFinishedAt());
        assertEquals("Twelve samples collected", tracking.getResult());
        assertTrue(item.isFinished());
    }

    @Test
    void recordsObservationsAndIncidentsOfAnActivityInProgress() {
        inExecution().startActivity(item, at(9, 0));

        expedition.recordObservation(item, new Observation("Water clearer than expected", at(9, 30)));
        expedition.reportIncident(item, new Incident("One sample jar broke", at(10, 0)));

        final var tracking = item.getTracking().orElseThrow();
        assertEquals(List.of(new Observation("Water clearer than expected", at(9, 30))), tracking.getObservations());
        assertEquals(List.of(new Incident("One sample jar broke", at(10, 0))), tracking.getIncidents());
    }

    @Test
    void finishingAnActivityConsumesTheSuppliesItWasAssigned() {
        inExecution().startActivity(item, at(9, 0));

        expedition.finishActivity(item, at(10, 0), "Done");

        assertEquals(Quantity.of("7", UNITS), jars.getStock());
    }

    @Test
    void activitiesCannotBeTrackedBeforeTheExpeditionStarts() {
        expedition.assignStaff(item, person("p-2"));
        expedition.submitForReview();
        expedition.approve(WITHOUT_PROBLEMS, List.of());

        assertThrows(InvalidTrackingException.class, () -> expedition.startActivity(item, at(9, 0)));
    }

    @Test
    void anActivityCannotStartTwice() {
        inExecution().startActivity(item, at(9, 0));

        assertThrows(InvalidTrackingException.class, () -> expedition.startActivity(item, at(9, 30)));
    }

    @Test
    void anActivityThatNeverStartedCannotBeFinishedOrCommented() {
        inExecution();

        assertThrows(InvalidTrackingException.class, () -> expedition.finishActivity(item, at(10, 0), "Done"));
        assertThrows(InvalidTrackingException.class,
                () -> expedition.recordObservation(item, new Observation("Nothing to say", at(10, 0))));
    }

    @Test
    void anActivityCannotFinishBeforeItStartsNorTwice() {
        inExecution().startActivity(item, at(9, 0));

        assertThrows(InvalidTrackingException.class, () -> expedition.finishActivity(item, at(8, 0), "Done"));

        expedition.finishActivity(item, at(10, 0), "Done");
        assertThrows(InvalidTrackingException.class, () -> expedition.finishActivity(item, at(11, 0), "Done again"));
    }

    @Test
    void anActivityStartedOutsideItsScheduledPeriodIsFlagged() {
        inExecution();

        assertFalse(item.startedOutOfSchedule());

        expedition.startActivity(item, at(12, 0));
        assertTrue(item.startedOutOfSchedule());
    }

    @Test
    void aFailedConsumptionLeavesTheActivityInProgressAndTheStockUntouched() {
        final var scarceExpedition = expeditionIn(coast, person("p-1")).build();
        final var scarceItem = scarceExpedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(9, 11));
        final var scarceJars = supply("d-2", "Sample container", Quantity.of("2", UNITS));
        scarceExpedition.assignSupply(scarceItem, scarceJars);
        scarceExpedition.submitForReview();
        scarceExpedition.approve(WITHOUT_PROBLEMS, List.of());
        scarceExpedition.start();
        scarceExpedition.startActivity(scarceItem, at(9, 0));

        assertThrows(InsufficientStockException.class,
                () -> scarceExpedition.finishActivity(scarceItem, at(11, 0), "Samples taken"));

        assertTrue(scarceItem.isInProgress());
        assertEquals(Quantity.of("2", UNITS), scarceJars.getStock());
    }

    @Test
    void anActivityThatCannotConsumeEverySupplyConsumesNone() {
        final var multiSupplyExpedition = expeditionIn(coast, person("p-1")).build();
        final var activity = new Activity(new ActivityId("a-1"), "Survey with boat", hours(0, 24), coast, List.of(), new FuelAndContainersRules());
        final var multiSupplyItem = multiSupplyExpedition.schedule(activity, hours(9, 11));
        final var fuel = supply("d-2", "Fuel", Quantity.of("50", LITERS));
        final var scarceJars = supply("d-3", "Sample container", Quantity.of("2", UNITS));
        multiSupplyExpedition.assignSupply(multiSupplyItem, fuel);
        multiSupplyExpedition.assignSupply(multiSupplyItem, scarceJars);
        multiSupplyExpedition.submitForReview();
        multiSupplyExpedition.approve(WITHOUT_PROBLEMS, List.of());
        multiSupplyExpedition.start();
        multiSupplyExpedition.startActivity(multiSupplyItem, at(9, 0));

        assertThrows(InsufficientStockException.class,
                () -> multiSupplyExpedition.finishActivity(multiSupplyItem, at(11, 0), "Samples taken"));

        assertEquals(Quantity.of("50", LITERS), fuel.getStock());
        assertEquals(Quantity.of("2", UNITS), scarceJars.getStock());
    }

    private static final class FuelAndContainersRules implements ActivityRules {

        @Override
        public Duration estimatedDuration() {
            return Duration.ofHours(2);
        }

        @Override
        public RiskLevel estimatedRisk() {
            return RiskLevel.LOW;
        }

        @Override
        public List<ReusableRequirement> requiredEquipment() {
            return List.of();
        }

        @Override
        public List<DepletableRequirement> requiredSupplies() {
            return List.of(
                    new DepletableRequirement(new ResourceCategory("Fuel"), Quantity.of("20", LITERS)),
                    new DepletableRequirement(new ResourceCategory("Sample container"), Quantity.of("3", UNITS)));
        }

        @Override
        public List<StaffRequirement> requiredStaff() {
            return List.of();
        }
    }
}
