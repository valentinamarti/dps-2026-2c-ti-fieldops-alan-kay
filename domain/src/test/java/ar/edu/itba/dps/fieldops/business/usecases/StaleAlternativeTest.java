package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.StaleAlternativeException;
import ar.edu.itba.dps.fieldops.business.interfaces.resources.DepletableResource;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentReport;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Observation;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalStatus;
import ar.edu.itba.dps.fieldops.business.replanning.Delay;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaleAlternativeTest {
    private final IncidentScenario scenario = new IncidentScenario();
    private final QueryExpeditions queryExpeditions = new QueryExpeditions(scenario.expeditions);

    private static LocalDateTime at(int hour) {
        return LocalDateTime.of(2026, 10, 1, hour, 0);
    }

    private Expedition alternativeForAStormOnTheAfternoon() {
        final var proposal = scenario.registerIncident.register(scenario.incidentOn(scenario.afternoon,
                IncidentReport.of("Storm forecast", at(8)).delayingBy(Duration.ofHours(1))));
        return scenario.decide.accept(proposal.getId(), 1, scenario.decisionBy(scenario.leader, "We wait one hour"));
    }

    private void run(Activity activity, int fromHour, int toHour) {
        final var item = scenario.itemOf(scenario.expedition, activity);
        scenario.expedition.startActivity(item, at(fromHour));
        scenario.expedition.finishActivity(item, at(toHour), "Done");
    }

    private void assertCannotBeApproved(Expedition alternative) {
        alternative.submitForReview();

        assertThrows(StaleAlternativeException.class,
                () -> scenario.approveExpedition.approve(alternative.getId(), List.of()));

        assertEquals(ExpeditionStatus.SUSPENDED, scenario.expedition.getStatus());
        assertEquals(ExpeditionStatus.IN_REVIEW, alternative.getStatus());
        assertEquals(Optional.empty(), scenario.expedition.getReplacement());
    }

    @Test
    void anActivityFinishedAfterResumingMakesTheAlternativeStaleAndItsSuppliesAreNotReservedAgain() {
        final var alternative = alternativeForAStormOnTheAfternoon();
        final var transitions = new TransitionExpedition(scenario.expeditions);
        final var tracking = new TrackActivity(scenario.expeditions);
        final DepletableResource jars = scenario.itemOf(scenario.expedition, scenario.morning)
                .getAssignedSupplies().iterator().next();

        transitions.resume(scenario.expedition.getId());
        tracking.start(scenario.expedition.getId(), scenario.morning.getId(), at(9));
        tracking.finish(scenario.expedition.getId(), scenario.morning.getId(), at(11), "All samples taken");
        transitions.suspend(scenario.expedition.getId());

        assertCannotBeApproved(alternative);
        assertTrue(jars.hasStockFor(Quantity.of("7", UNITS)));
    }

    @Test
    void theAffectedActivityFinishedAfterResumingMakesTheAlternativeStale() {
        final var alternative = alternativeForAStormOnTheAfternoon();

        scenario.expedition.resume();
        run(scenario.afternoon, 13, 15);
        scenario.expedition.suspend();

        assertCannotBeApproved(alternative);
    }

    @Test
    void anObservationRecordedAfterResumingMakesTheAlternativeStale() {
        run(scenario.morning, 9, 11);
        final var alternative = alternativeForAStormOnTheAfternoon();

        scenario.expedition.resume();
        scenario.expedition.recordObservation(scenario.itemOf(scenario.expedition, scenario.morning),
                new Observation("The samples were labelled again", at(12)));
        scenario.expedition.suspend();

        assertCannotBeApproved(alternative);
    }

    @Test
    void aSecondIncidentMakesTheFirstAlternativeStaleAndLeavesTheSecondOneApprovable() {
        final var first = alternativeForAStormOnTheAfternoon();
        scenario.expedition.resume();
        final var second = scenario.registerIncident.register(scenario.incidentOn(scenario.morning,
                IncidentReport.of("Boat engine failed", at(8)).delayingBy(Duration.ofMinutes(30))));
        final var secondAlternative = scenario.decide.accept(second.getId(), 1,
                scenario.decisionBy(scenario.leader, "The mechanic is on site"));

        assertCannotBeApproved(first);
        secondAlternative.submitForReview();
        scenario.approveExpedition.approve(secondAlternative.getId(), List.of());

        assertEquals(ExpeditionStatus.APPROVED, secondAlternative.getStatus());
        assertEquals(ExpeditionStatus.REPLACED, scenario.expedition.getStatus());
        assertEquals(ReplanProposalStatus.ACCEPTED, second.getStatus());
        assertEquals(scenario.expedition.getIncidents(), secondAlternative.getIncidents());
    }

    @Test
    void anAlternativeBuiltWithoutAProposalBecomesStaleToo() {
        scenario.expedition.suspend();
        final var alternative = scenario.replanExpedition.replan(scenario.expedition.getId(),
                scenario.afternoon.getId(), new Delay(Duration.ofHours(1)));

        scenario.expedition.resume();
        run(scenario.morning, 9, 11);
        scenario.expedition.suspend();

        assertCannotBeApproved(alternative);
    }

    @Test
    void resumingAndSuspendingWithoutExecutingAnythingKeepsTheAlternativeApprovable() {
        final var alternative = alternativeForAStormOnTheAfternoon();

        scenario.expedition.resume();
        scenario.expedition.suspend();
        alternative.submitForReview();
        scenario.approveExpedition.approve(alternative.getId(), List.of());

        assertEquals(ExpeditionStatus.APPROVED, alternative.getStatus());
        assertEquals(ExpeditionStatus.REPLACED, scenario.expedition.getStatus());
    }

    @Test
    void theQueryTellsWhetherAnExpeditionIsAStaleAlternative() {
        final var alternative = alternativeForAStormOnTheAfternoon();

        assertFalse(queryExpeditions.isStaleAlternative(scenario.expedition));
        assertFalse(queryExpeditions.isStaleAlternative(alternative));

        scenario.expedition.resume();
        run(scenario.morning, 9, 11);

        assertFalse(queryExpeditions.isStaleAlternative(scenario.expedition));
        assertTrue(queryExpeditions.isStaleAlternative(alternative));
    }
}
