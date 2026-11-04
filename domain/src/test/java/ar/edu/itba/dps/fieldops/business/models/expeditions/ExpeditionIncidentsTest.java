package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.exceptions.ActivitiesInProgressException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidTrackingException;
import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.events.IncidentRegistered;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.APPROVED_AT;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.completeAssignments;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExpeditionIncidentsTest {

    private final SequentialExpeditionRepository repository = new SequentialExpeditionRepository();
    private final Zone coast = zone("z-1");
    private final Expedition expedition = expeditionIn(coast, person("p-1")).build(repository);
    private final ItineraryItem morning = expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(9, 11));
    private final ItineraryItem afternoon = expedition.schedule(sampling("a-2", coast, hours(0, 24)), hours(13, 15));

    private static LocalDateTime at(int hour) {
        return LocalDateTime.of(2026, 10, 1, hour, 0);
    }

    private Expedition inExecution() {
        completeAssignments(expedition);
        expedition.submitForReview();
        expedition.approve(List.of(), List.of(), APPROVED_AT);
        expedition.start();
        expedition.pullEvents();
        return expedition;
    }

    @Test
    void registersAnIncidentOnAnActivityThatHasNotStartedYet() {
        final var incident = inExecution().registerIncident(afternoon, IncidentReport.of("Storm forecast for the afternoon", at(10)));

        assertEquals(new ActivityId("a-2"), incident.activity());
        assertEquals("Storm forecast for the afternoon", incident.description());
        assertEquals(at(10), incident.occurredAt());
        assertEquals(List.of(incident), expedition.getIncidents());
    }

    @Test
    void registersAnIncidentOnAnActivityInProgress() {
        inExecution().startActivity(morning, at(9));

        final var incident = expedition.registerIncident(morning, IncidentReport.of("Current stronger than forecast", at(10)));

        assertEquals(new ActivityId("a-1"), incident.activity());
    }

    @Test
    void numbersTheIncidentsOfTheExpeditionInTheOrderTheyAreRegistered() {
        final var first = inExecution().registerIncident(morning, IncidentReport.of("One jar broke", at(9)));
        final var second = expedition.registerIncident(afternoon, IncidentReport.of("Storm forecast", at(10)));

        assertEquals(new IncidentId(expedition.getId(), 1), first.id());
        assertEquals(new IncidentId(expedition.getId(), 2), second.id());
    }

    @Test
    void keepsTheResourceAndTheDelayTheReportNames() {
        inExecution();
        final var worker = afternoon.getAssignedStaff().iterator().next();

        final var incident = expedition.registerIncident(afternoon, IncidentReport.of("Sick", at(10))
                .affecting(worker.getId())
                .delayingBy(Duration.ofHours(2)));

        assertEquals(Optional.of(worker.getId()), incident.resource());
        assertEquals(Optional.of(Duration.ofHours(2)), incident.expectedDelay());
    }

    @Test
    void registeringAnIncidentRecordsTheEventOfTheFact() {
        final var incident = inExecution().registerIncident(afternoon, IncidentReport.of("Storm forecast", at(10)));

        final var events = expedition.pullEvents();

        assertEquals(1, events.size());
        final var event = assertInstanceOf(IncidentRegistered.class, events.getFirst());
        assertEquals(expedition.getId(), event.expeditionId());
        assertEquals(incident, event.incident());
        assertEquals(at(10), event.occurredAt());
    }

    @Test
    void theEventsAreTakenOnlyOnce() {
        inExecution().registerIncident(afternoon, IncidentReport.of("Storm forecast", at(10)));
        expedition.pullEvents();

        assertEquals(List.of(), expedition.pullEvents());
    }

    @Test
    void eachIncidentRecordsItsOwnEventInTheOrderTheyHappen() {
        final var first = inExecution().registerIncident(morning, IncidentReport.of("One jar broke", at(9)));
        final var second = expedition.registerIncident(afternoon, IncidentReport.of("Storm forecast", at(10)));

        final var events = expedition.pullEvents();

        assertEquals(List.of(first, second), events.stream().map(IncidentRegistered.class::cast).map(IncidentRegistered::incident).toList());
        assertNotEquals(events.get(0).id(), events.get(1).id());
    }

    @Test
    void aRestoredExpeditionDoesNotBringBackTheEventsOfTheOneItCameFrom() {
        inExecution().registerIncident(afternoon, IncidentReport.of("Storm forecast", at(10)));

        final var restored = Expedition.restore(expedition.snapshot());

        assertEquals(List.of(), restored.pullEvents());
    }

    @Test
    void rejectsAResourceThatIsNotAssignedToTheActivity() {
        final var report = IncidentReport.of("Engine failure", at(10)).affecting(new ResourceId("v-99"));

        assertThrows(InvalidTrackingException.class, () -> inExecution().registerIncident(afternoon, report));
        assertEquals(List.of(), expedition.getIncidents());
        assertEquals(List.of(), expedition.pullEvents());
    }

    @Test
    void rejectsAnIncidentOnAnActivityThatAlreadyFinished() {
        inExecution().startActivity(morning, at(9));
        expedition.finishActivity(morning, at(11), "Samples taken");

        assertThrows(InvalidTrackingException.class,
                () -> expedition.registerIncident(morning, IncidentReport.of("Too late", at(12))));
    }

    @Test
    void onlyAnExpeditionInExecutionRegistersIncidents() {
        assertThrows(InvalidTrackingException.class,
                () -> expedition.registerIncident(morning, IncidentReport.of("Storm forecast", at(8))));
        assertEquals(List.of(), expedition.pullEvents());
    }

    @Test
    void anExpectedDelayMustBePositive() {
        final var report = IncidentReport.of("Engine failure", at(10));

        assertThrows(IllegalArgumentException.class, () -> report.delayingBy(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> report.delayingBy(Duration.ofMinutes(-5)));
    }

    @Test
    void anIncidentNumberStartsAtOne() {
        assertThrows(IllegalArgumentException.class, () -> new IncidentId(expedition.getId(), 0));
    }

    @Test
    void suspendToReplanSuspendsAnExpeditionInExecution() {
        inExecution().suspendToReplan();

        assertEquals(ExpeditionStatus.SUSPENDED, expedition.getStatus());
    }

    @Test
    void suspendToReplanChangesNothingWhileAnActivityIsInProgress() {
        inExecution().startActivity(morning, at(9));

        assertThrows(ActivitiesInProgressException.class, expedition::suspendToReplan);
        assertEquals(ExpeditionStatus.IN_EXECUTION, expedition.getStatus());
    }
}
