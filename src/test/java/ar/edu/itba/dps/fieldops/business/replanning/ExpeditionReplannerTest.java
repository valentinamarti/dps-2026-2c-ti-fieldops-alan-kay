package ar.edu.itba.dps.fieldops.business.replanning;

import ar.edu.itba.dps.fieldops.business.exceptions.ActivitiesInProgressException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidScheduleException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidStatusTransitionException;
import ar.edu.itba.dps.fieldops.business.interfaces.validation.ExpeditionValidator;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.validation.ApprovalResult;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.services.ResourceReservations;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.EXPEDITION_ID;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpeditionReplannerTest {

    private static final ExpeditionValidator WITHOUT_PROBLEMS = _ -> new ApprovalResult(List.of());
    private static final ExpeditionId ALTERNATIVE_ID = new ExpeditionId("e-1-alt");

    private final ExpeditionReplanner replanner = new ExpeditionReplanner();
    private final Zone coast = zone("z-1");
    private final Person leader = person("p-1");
    private final Expedition expedition = expeditionIn(coast, leader).build(EXPEDITION_ID);

    private void approve() {
        expedition.submitForReview();
        expedition.approve(WITHOUT_PROBLEMS, List.of());
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

        final var alternative = replanner.planAlternative(expedition, ALTERNATIVE_ID, affectedItem);

        assertEquals(ExpeditionStatus.DRAFT, alternative.getStatus());
        assertEquals(1, alternative.getItinerary().getItems().size());
        final var copiedItem = alternative.getItinerary().itemFor(keptActivity).orElseThrow();
        assertEquals(Set.of(worker), copiedItem.getAssignedStaff());
        assertTrue(alternative.getItinerary().itemFor(affectedActivity).isEmpty());
    }

    @Test
    void rejectsAnItemThatDoesNotBelongToTheOriginalExpedition() {
        final var otherExpedition = expeditionIn(coast, leader).build(EXPEDITION_ID);
        final var foreignItem = otherExpedition.schedule(sampling("a-1", coast, hours(8, 18)), hours(9, 11));
        approve();

        assertThrows(IllegalArgumentException.class, () -> replanner.planAlternative(expedition, ALTERNATIVE_ID, foreignItem));
    }

    @Test
    void failsWhenAScheduledActivityDependsOnTheAffectedOne() {
        final var dependency = sampling("a-1", coast, hours(8, 18));
        final var dependent = sampling("a-2", coast, hours(8, 18), dependency);
        final var affectedItem = expedition.schedule(dependency, hours(9, 11));
        expedition.schedule(dependent, hours(12, 14));
        approve();

        assertThrows(InvalidScheduleException.class, () -> replanner.planAlternative(expedition, ALTERNATIVE_ID, affectedItem));
    }

    @Test
    void aDraftIsEditedDirectlyInsteadOfReplanned() {
        final var item = expedition.schedule(sampling("a-1", coast, hours(8, 18)), hours(9, 11));

        assertThrows(InvalidStatusTransitionException.class, () -> replanner.planAlternative(expedition, ALTERNATIVE_ID, item));
    }

    @Test
    void anExpeditionInExecutionHasToBeSuspendedBeforeBeingReplanned() {
        final var item = expedition.schedule(sampling("a-1", coast, hours(8, 18)), hours(9, 11));
        approve();
        expedition.start();

        assertThrows(InvalidStatusTransitionException.class, () -> replanner.planAlternative(expedition, ALTERNATIVE_ID, item));
    }

    @Test
    void aSuspendedExpeditionCanBeReplanned() {
        expedition.schedule(sampling("a-1", coast, hours(8, 18)), hours(9, 11));
        final var affectedItem = expedition.schedule(sampling("a-2", coast, hours(8, 18)), hours(12, 14));
        approve();
        expedition.start();
        expedition.suspend();

        final var alternative = replanner.planAlternative(expedition, ALTERNATIVE_ID, affectedItem);

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

        assertThrows(ActivitiesInProgressException.class, () -> replanner.planAlternative(expedition, ALTERNATIVE_ID, affectedItem));
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

        final var alternative = replanner.planAlternative(expedition, ALTERNATIVE_ID, affectedItem);

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
        final var alternative = replanner.planAlternative(expedition, ALTERNATIVE_ID, affectedItem);

        new ResourceReservations().reserveFor(alternative);

        assertTrue(worker.isAvailableDuring(hours(9, 11)));
    }
}
