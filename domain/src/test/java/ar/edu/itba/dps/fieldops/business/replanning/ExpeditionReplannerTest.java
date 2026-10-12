package ar.edu.itba.dps.fieldops.business.replanning;

import ar.edu.itba.dps.fieldops.business.exceptions.ActivitiesInProgressException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidScheduleException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidStatusTransitionException;
import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.services.ResourceReservations;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.completeAssignments;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.activity;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.parallel;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.sequential;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpeditionReplannerTest {


    private final SequentialExpeditionRepository repository = new SequentialExpeditionRepository();
    private final ExpeditionReplanner replanner = new ExpeditionReplanner(repository);
    private final Zone coast = zone("z-1");
    private final Person leader = person("p-1");
    private final Expedition expedition = expeditionIn(coast, leader).build(repository);

    private void approve() {
        completeAssignments(expedition);
        expedition.submitForReview();
        expedition.approve(List.of(), List.of());
    }

    @Test
    void keepsUnaffectedItemsWithTheirAssignmentsAndDropsTheAffectedOne() {
        final var keptActivity = sampling("a-1", coast, hours(8, 18));
        final var affectedActivity = sampling("a-2", coast, hours(8, 18));
        final var keptItem = expedition.schedule(keptActivity, hours(9, 11));
        final var affectedItem = expedition.schedule(affectedActivity, hours(12, 14));
        final var worker = person("p-2");
        expedition.assignStaff(keptItem, worker);
        approve();

        final var alternative = replanner.planAlternative(expedition, affectedItem, new Cancellation());

        assertEquals(ExpeditionStatus.DRAFT, alternative.getStatus());
        assertEquals(1, alternative.getItinerary().getItems().size());
        final var copiedItem = alternative.getItinerary().itemFor(keptActivity).orElseThrow();
        assertEquals(Set.of(worker), copiedItem.getAssignedStaff());
        assertTrue(alternative.getItinerary().itemFor(affectedActivity).isEmpty());
    }

    @Test
    void rejectsAnItemThatDoesNotBelongToTheOriginalExpedition() {
        final var otherExpedition = expeditionIn(coast, leader).build(repository);
        final var foreignItem = otherExpedition.schedule(sampling("a-1", coast, hours(8, 18)), hours(9, 11));
        approve();

        assertThrows(IllegalArgumentException.class, () -> replanner.planAlternative(expedition, foreignItem, new Cancellation()));
    }

    @Test
    void failsWhenAScheduledActivityDependsOnTheAffectedOne() {
        final var dependency = sampling("a-1", coast, hours(8, 18));
        final var dependent = sampling("a-2", coast, hours(8, 18), dependency);
        final var affectedItem = expedition.schedule(dependency, hours(9, 11));
        expedition.schedule(dependent, hours(12, 14));
        approve();

        assertThrows(InvalidScheduleException.class, () -> replanner.planAlternative(expedition, affectedItem, new Cancellation()));
    }

    @Test
    void aDraftIsEditedDirectlyInsteadOfReplanned() {
        final var item = expedition.schedule(sampling("a-1", coast, hours(8, 18)), hours(9, 11));

        assertThrows(InvalidStatusTransitionException.class, () -> replanner.planAlternative(expedition, item, new Cancellation()));
    }

    @Test
    void anExpeditionInExecutionHasToBeSuspendedBeforeBeingReplanned() {
        final var item = expedition.schedule(sampling("a-1", coast, hours(8, 18)), hours(9, 11));
        approve();
        expedition.start();

        assertThrows(InvalidStatusTransitionException.class, () -> replanner.planAlternative(expedition, item, new Cancellation()));
    }

    @Test
    void aSuspendedExpeditionCanBeReplanned() {
        expedition.schedule(sampling("a-1", coast, hours(8, 18)), hours(9, 11));
        final var affectedItem = expedition.schedule(sampling("a-2", coast, hours(8, 18)), hours(12, 14));
        approve();
        expedition.start();
        expedition.suspend();

        final var alternative = replanner.planAlternative(expedition, affectedItem, new Cancellation());

        assertEquals(ExpeditionStatus.DRAFT, alternative.getStatus());
        assertEquals(ExpeditionStatus.SUSPENDED, expedition.getStatus());
    }

    @Test
    void anExpeditionWithAnActivityInProgressCannotBeReplanned() {
        final var runningItem = expedition.schedule(sampling("a-1", coast, hours(8, 18)), hours(9, 11));
        final var affectedItem = expedition.schedule(sampling("a-2", coast, hours(8, 18)), hours(12, 14));
        approve();
        expedition.start();
        expedition.startActivity(runningItem, LocalDateTime.of(2026, 10, 1, 9, 0));
        expedition.suspend();

        assertThrows(ActivitiesInProgressException.class, () -> replanner.planAlternative(expedition, affectedItem, new Cancellation()));
    }

    @Test
    void theAlternativeKeepsWhatAlreadyHappenedAndCanOnlyChangeWhatIsPending() {
        final var done = sampling("a-1", coast, hours(0, 24));
        final var pending = sampling("a-2", coast, hours(0, 24), done);
        final var affected = sampling("a-3", coast, hours(0, 24));
        final var doneItem = expedition.schedule(done, hours(9, 11));
        expedition.schedule(pending, hours(12, 14));
        final var affectedItem = expedition.schedule(affected, hours(15, 17));
        final var worker = person("p-2");
        expedition.assignStaff(doneItem, worker);
        approve();
        expedition.start();
        expedition.startActivity(doneItem, LocalDateTime.of(2026, 10, 1, 9, 0));
        expedition.finishActivity(doneItem, LocalDateTime.of(2026, 10, 1, 11, 0), "Samples taken");
        expedition.suspend();

        final var alternative = replanner.planAlternative(expedition, affectedItem, new Cancellation());

        final var keptDone = alternative.getItinerary().itemFor(done).orElseThrow();
        final var keptPending = alternative.getItinerary().itemFor(pending).orElseThrow();
        assertTrue(keptDone.isFinished());
        assertEquals("Samples taken", keptDone.getTracking().orElseThrow().getResult());
        assertEquals(Set.of(worker), keptDone.getAssignedStaff());
        assertEquals(List.of(keptPending), alternative.getItinerary().pendingItems());
        assertThrows(InvalidScheduleException.class, () -> alternative.assignStaff(keptDone, person("p-3")));
        alternative.assignStaff(keptPending, person("p-3"));
        assertTrue(alternative.getItinerary().itemFor(affected).isEmpty());
    }

    @Test
    void whatAlreadyHappenedIsNotReservedAgain() {
        final var doneItem = expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(9, 11));
        final var affectedItem = expedition.schedule(sampling("a-2", coast, hours(0, 24)), hours(12, 14));
        final var worker = person("p-2");
        expedition.assignStaff(doneItem, worker);
        approve();
        expedition.start();
        expedition.startActivity(doneItem, LocalDateTime.of(2026, 10, 1, 9, 0));
        expedition.finishActivity(doneItem, LocalDateTime.of(2026, 10, 1, 11, 0), "Samples taken");
        expedition.suspend();
        final var alternative = replanner.planAlternative(expedition, affectedItem, new Cancellation());

        new ResourceReservations().reserveFor(alternative);

        assertTrue(worker.isAvailableDuring(hours(9, 11)));
    }

    @Test
    void aDelayRunsTheAffectedActivityLaterWithTheSameResources() {
        final var affectedActivity = sampling("a-1", coast, hours(8, 18));
        final var affectedItem = expedition.schedule(affectedActivity, hours(12, 14));
        final var worker = person("p-2");
        expedition.assignStaff(affectedItem, worker);
        approve();

        final var alternative = replanner.planAlternative(expedition, affectedItem, new Delay(Duration.ofHours(2)));

        final var delayed = alternative.getItinerary().itemFor(affectedActivity).orElseThrow();
        assertEquals(hours(14, 16), delayed.getScheduledPeriod());
        assertEquals(Set.of(worker), delayed.getAssignedStaff());
    }

    @Test
    void aDelayThatLeavesTheActivityWindowCannotBePlanned() {
        final var affectedItem = expedition.schedule(sampling("a-1", coast, hours(8, 18)), hours(15, 17));
        approve();

        assertThrows(InvalidScheduleException.class,
                () -> replanner.planAlternative(expedition, affectedItem, new Delay(Duration.ofHours(2))));
    }

    @Test
    void aResourceWithdrawnKeepsTheActivityWithoutThatResource() {
        final var affectedActivity = sampling("a-1", coast, hours(8, 18));
        final var affectedItem = expedition.schedule(affectedActivity, hours(12, 14));
        final var sickWorker = person("p-2");
        final var otherWorker = person("p-3");
        expedition.assignStaff(affectedItem, sickWorker);
        expedition.assignStaff(affectedItem, otherWorker);
        approve();

        final var alternative = replanner.planAlternative(expedition, affectedItem,
                new ResourceWithdrawn(sickWorker.getId()));

        final var kept = alternative.getItinerary().itemFor(affectedActivity).orElseThrow();
        assertEquals(hours(12, 14), kept.getScheduledPeriod());
        assertEquals(Set.of(otherWorker), kept.getAssignedStaff());
    }

    @Test
    void theAlternativeRecordsWhyItExists() {
        final var affectedItem = expedition.schedule(sampling("a-1", coast, hours(8, 18)), hours(12, 14));
        approve();
        final var reason = new Delay(Duration.ofHours(1));

        final var alternative = replanner.planAlternative(expedition, affectedItem, reason);

        assertEquals(Optional.of(reason), alternative.getReplanReason());
    }

    @Test
    void anActivityThatAlreadyHappenedCannotBeTheAffectedOne() {
        final var doneItem = expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(9, 11));
        approve();
        expedition.start();
        expedition.startActivity(doneItem, LocalDateTime.of(2026, 10, 1, 9, 0));
        expedition.finishActivity(doneItem, LocalDateTime.of(2026, 10, 1, 11, 0), "Samples taken");
        expedition.suspend();

        assertThrows(IllegalArgumentException.class,
                () -> replanner.planAlternative(expedition, doneItem, new Delay(Duration.ofHours(1))));
    }

    @Test
    void aDelayMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new Delay(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new Delay(Duration.ofHours(-1)));
    }

    @Test
    void theAlternativeKeepsTheBlocksOfTheOriginal() {
        final var landing = sampling("a-1", coast, hours(0, 24));
        final var dive = sampling("a-2", coast, hours(0, 24));
        final var transect = sampling("a-3", coast, hours(0, 24));
        expedition.schedule(sequential(
                activity(landing, hours(8, 9)),
                parallel(activity(dive, hours(9, 11)), activity(transect, hours(9, 11)))));
        final var affectedItem = expedition.schedule(sampling("a-4", coast, hours(0, 24)), hours(12, 14));
        final var worker = person("p-2");
        expedition.assignStaff(expedition.getItinerary().itemFor(dive).orElseThrow(), worker);
        approve();

        final var alternative = replanner.planAlternative(expedition, affectedItem, new Cancellation());

        final var itinerary = alternative.getItinerary();
        final var keptDive = itinerary.itemFor(dive).orElseThrow();
        assertEquals(1, itinerary.getNodes().size());
        assertEquals(hours(8, 11), itinerary.getNodes().getFirst().getScheduledPeriod());
        assertEquals(3, itinerary.getItems().size());
        assertTrue(itinerary.runInParallel(keptDive, itinerary.itemFor(transect).orElseThrow()));
        assertFalse(itinerary.runInParallel(itinerary.itemFor(landing).orElseThrow(), keptDive));
        assertEquals(Duration.ofHours(4), itinerary.getNodes().getFirst().estimatedDuration());
        assertEquals(Set.of(worker), keptDive.getAssignedStaff());
    }

    @Test
    void anActivityThatAlreadyHappenedStaysClosedInsideItsBlock() {
        final var done = sampling("a-1", coast, hours(0, 24));
        final var pending = sampling("a-2", coast, hours(0, 24));
        expedition.schedule(sequential(activity(done, hours(9, 11)), activity(pending, hours(12, 14))));
        final var affectedItem = expedition.schedule(sampling("a-3", coast, hours(0, 24)), hours(15, 17));
        final var doneItem = expedition.getItinerary().itemFor(done).orElseThrow();
        approve();
        expedition.start();
        expedition.startActivity(doneItem, LocalDateTime.of(2026, 10, 1, 9, 0));
        expedition.finishActivity(doneItem, LocalDateTime.of(2026, 10, 1, 11, 0), "Samples taken");
        expedition.suspend();

        final var alternative = replanner.planAlternative(expedition, affectedItem, new Cancellation());

        final var itinerary = alternative.getItinerary();
        assertEquals(1, itinerary.getNodes().size());
        assertEquals(hours(9, 14), itinerary.getNodes().getFirst().getScheduledPeriod());
        assertTrue(itinerary.itemFor(done).orElseThrow().isFinished());
        assertEquals(List.of(itinerary.itemFor(pending).orElseThrow()), itinerary.pendingItems());
    }

    @Test
    void cancellingAnActivityOfABlockKeepsTheRestOfTheBlock() {
        final var kept = sampling("a-1", coast, hours(0, 24));
        final var affected = sampling("a-2", coast, hours(0, 24));
        expedition.schedule(sampling("a-3", coast, hours(0, 24)), hours(6, 8));
        expedition.schedule(parallel(activity(kept, hours(9, 11)), activity(affected, hours(9, 13))));
        approve();

        final var alternative = replanner.planAlternative(expedition,
                expedition.getItinerary().itemFor(affected).orElseThrow(), new Cancellation());

        final var itinerary = alternative.getItinerary();
        assertEquals(2, itinerary.getNodes().size());
        assertEquals(hours(9, 11), itinerary.getNodes().getLast().getScheduledPeriod());
        assertTrue(itinerary.itemFor(kept).isPresent());
        assertTrue(itinerary.itemFor(affected).isEmpty());
    }

    @Test
    void cancellingTheOnlyActivityOfABlockDropsTheBlock() {
        final var affected = sampling("a-2", coast, hours(0, 24));
        final var looseItem = expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(6, 8));
        expedition.schedule(sequential(parallel(activity(affected, hours(9, 11)))));
        approve();

        final var alternative = replanner.planAlternative(expedition,
                expedition.getItinerary().itemFor(affected).orElseThrow(), new Cancellation());

        final var itinerary = alternative.getItinerary();
        assertEquals(1, itinerary.getNodes().size());
        assertEquals(List.of(itinerary.itemFor(looseItem.getActivity()).orElseThrow()), itinerary.getItems());
    }

    @Test
    void aDelayedActivityStaysInItsBlock() {
        final var affected = sampling("a-1", coast, hours(0, 24));
        final var other = sampling("a-2", coast, hours(0, 24));
        expedition.schedule(parallel(activity(affected, hours(9, 11)), activity(other, hours(9, 11))));
        approve();

        final var alternative = replanner.planAlternative(expedition,
                expedition.getItinerary().itemFor(affected).orElseThrow(), new Delay(Duration.ofHours(2)));

        final var itinerary = alternative.getItinerary();
        final var delayed = itinerary.itemFor(affected).orElseThrow();
        assertEquals(hours(11, 13), delayed.getScheduledPeriod());
        assertEquals(hours(9, 13), itinerary.getNodes().getFirst().getScheduledPeriod());
        assertTrue(itinerary.runInParallel(delayed, itinerary.itemFor(other).orElseThrow()));
    }

    @Test
    void aDelayThatBreaksTheOrderOfASequentialBlockCannotBePlanned() {
        final var affected = sampling("a-1", coast, hours(0, 24));
        expedition.schedule(sequential(
                activity(affected, hours(9, 11)),
                activity(sampling("a-2", coast, hours(0, 24)), hours(11, 13))));
        approve();
        final var affectedItem = expedition.getItinerary().itemFor(affected).orElseThrow();

        assertThrows(InvalidScheduleException.class,
                () -> replanner.planAlternative(expedition, affectedItem, new Delay(Duration.ofHours(1))));
    }

    @Test
    void aBlockThatStartsBeforeADependencyOfOneOfItsActivitiesIsReplannedInTheOrderItWasScheduled() {
        final var dependency = sampling("a-1", coast, hours(0, 24));
        final var dependent = sampling("a-3", coast, hours(0, 24), dependency);
        expedition.schedule(dependency, hours(10, 12));
        expedition.schedule(sequential(
                activity(sampling("a-2", coast, hours(0, 24)), hours(8, 10)),
                activity(dependent, hours(13, 15))));
        final var affectedItem = expedition.schedule(sampling("a-4", coast, hours(0, 24)), hours(16, 18));
        approve();

        final var alternative = replanner.planAlternative(expedition, affectedItem, new Cancellation());

        assertEquals(2, alternative.getItinerary().getNodes().size());
        assertTrue(alternative.getItinerary().itemFor(dependent).isPresent());
    }
}
