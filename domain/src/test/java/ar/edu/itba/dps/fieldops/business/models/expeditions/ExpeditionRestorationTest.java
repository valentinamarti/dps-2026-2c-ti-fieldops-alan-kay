package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.responses.ItineraryView;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.replanning.Delay;
import ar.edu.itba.dps.fieldops.business.replanning.ExpeditionReplanner;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.completeAssignments;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.activity;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.parallel;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.sequential;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpeditionRestorationTest {
    private final SequentialExpeditionRepository ids = new SequentialExpeditionRepository();
    private final Zone coast = zone("z-1");
    private final Person leader = person("p-1");
    private final Activity first = sampling("a-1", coast, hours(0, 24));
    private final Activity second = sampling("a-2", coast, hours(0, 24));
    private final Activity third = sampling("a-3", coast, hours(0, 24));
    private final Expedition expedition = runningExpeditionWithABlock();

    private static LocalDateTime at(int hour) {
        return LocalDateTime.of(2026, 10, 1, hour, 0);
    }

    private Expedition runningExpeditionWithABlock() {
        final var running = expeditionIn(coast, leader).build(ids);
        running.schedule(sequential(
                activity(first, hours(8, 10)),
                parallel(activity(second, hours(10, 12)), activity(third, hours(10, 12)))));
        completeAssignments(running);
        running.submitForReview();
        running.approve(List.of(), List.of());
        running.start();
        final var item = running.getItinerary().itemFor(first).orElseThrow();
        running.startActivity(item, at(8));
        running.recordObservation(item, new Observation("Calm seas", at(9)));
        running.finishActivity(item, at(10), "Six samples");
        running.registerIncident(running.getItinerary().itemFor(second).orElseThrow(), IncidentReport.of("Storm", at(10)));
        return running;
    }

    @Test
    void aRestoredExpeditionHasTheStateOfTheOneItCameFrom() {
        final var restored = Expedition.restore(expedition.snapshot());

        assertEquals(expedition, restored);
        assertEquals(expedition.snapshot(), restored.snapshot());
        assertEquals(ExpeditionStatus.IN_EXECUTION, restored.getStatus());
        assertEquals(ItineraryView.of(expedition), ItineraryView.of(restored));
        assertEquals(expedition.getIncidents(), restored.getIncidents());
    }

    @Test
    void theTrackingOfAnActivityIsRestoredWithItsObservations() {
        final var tracking = Expedition.restore(expedition.snapshot()).getItinerary().itemFor(first).orElseThrow().getTracking().orElseThrow();

        assertEquals(at(8), tracking.getStartedAt());
        assertEquals(at(10), tracking.getFinishedAt());
        assertEquals("Six samples", tracking.getResult());
        assertEquals(List.of(new Observation("Calm seas", at(9))), tracking.getObservations());
    }

    @Test
    void aRestoredBlockStillRunsItsBranchesInParallel() {
        final var itinerary = Expedition.restore(expedition.snapshot()).getItinerary();

        assertTrue(itinerary.runInParallel(itinerary.itemFor(second).orElseThrow(), itinerary.itemFor(third).orElseThrow()));
        assertFalse(itinerary.runInParallel(itinerary.itemFor(first).orElseThrow(), itinerary.itemFor(second).orElseThrow()));
    }

    @Test
    void aRestoredExpeditionKeepsWorkingWithoutTouchingTheOriginalOne() {
        final var restored = Expedition.restore(expedition.snapshot());
        final var item = restored.getItinerary().itemFor(third).orElseThrow();

        restored.startActivity(item, at(10));

        assertTrue(item.isStarted());
        assertFalse(expedition.getItinerary().itemFor(third).orElseThrow().isStarted());
    }

    @Test
    void aRestoredAlternativeRemembersWhatItReplacesWhyAndOnWhichPlan() {
        expedition.suspend();
        final var affected = expedition.getItinerary().itemFor(third).orElseThrow();
        final var alternative = new ExpeditionReplanner(ids).planAlternative(expedition, affected, new Delay(Duration.ofHours(1)));
        final var restored = Expedition.restore(alternative.snapshot());
        final var original = Expedition.restore(expedition.snapshot());

        assertEquals(Optional.of(expedition.getId()), restored.getReplacedExpedition());
        assertEquals(Optional.of(new Delay(Duration.ofHours(1))), restored.getReplanReason());
        assertTrue(restored.isBuiltOnCurrentPlanOf(original));

        original.resume();
        original.startActivity(original.getItinerary().itemFor(second).orElseThrow(), at(10));

        assertFalse(restored.isBuiltOnCurrentPlanOf(original));
    }

    @Test
    void theRulesThatCompareEntitiesWorkWithAnInstanceLoadedLater() {
        final var restored = Expedition.restore(expedition.snapshot());

        assertDoesNotThrow(() -> restored.requireResponsible(person("p-1")));
        assertTrue(restored.getItinerary().itemFor(sampling("a-2", coast, hours(0, 24))).isPresent());
    }
}
