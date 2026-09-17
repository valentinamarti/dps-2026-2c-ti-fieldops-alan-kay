package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.exceptions.InvalidStatusTransitionException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidTrackingException;
import ar.edu.itba.dps.fieldops.business.interfaces.validation.ExpeditionValidator;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.resources.Depletable;
import ar.edu.itba.dps.fieldops.business.models.validation.ApprovalResult;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.supply;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpeditionTrackingTest {

    private static final ExpeditionValidator WITHOUT_PROBLEMS = expedition -> new ApprovalResult(List.of());

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
}
