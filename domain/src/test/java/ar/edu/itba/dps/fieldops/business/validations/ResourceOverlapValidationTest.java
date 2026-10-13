package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.Reservation;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.resources.Unavailability;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationSubject;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.services.ResourceReservations;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.activity;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.parallel;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.sequential;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceOverlapValidationTest {

    private static final ExpeditionId ANOTHER_EXPEDITION = new ExpeditionId("e-2");

    private final SequentialExpeditionRepository repository = new SequentialExpeditionRepository();
    private final ResourceOverlapValidation validation = new ResourceOverlapValidation();
    private final Zone coast = zone("z-1");
    private final Expedition expedition = expeditionIn(coast, person("p-1")).build(repository);
    private final Person worker = person("p-2");

    private void scheduleWithWorker(String activityId, TimePeriod period) {
        final var item = expedition.schedule(sampling(activityId, coast, hours(0, 24)), period);
        expedition.assignStaff(item, worker);
    }

    @Test
    void flagsAResourceAssignedToTwoActivitiesAtTheSameTime() {
        scheduleWithWorker("a-1", hours(9, 11));
        scheduleWithWorker("a-2", hours(10, 12));

        final var expected = ValidationResult.critical(ResourceOverlapValidation.ID, ValidationSubject.of(new ActivityId("a-1"), new ResourceId("p-2")), "p-2 is assigned to Sampling a-1 and Sampling a-2 at the same time");
        assertEquals(List.of(expected), validation.validate(expedition));
    }

    @Test
    void acceptsTheSameResourceInActivitiesThatDoNotOverlap() {
        scheduleWithWorker("a-1", hours(9, 11));
        scheduleWithWorker("a-2", hours(11, 13));

        assertTrue(validation.validate(expedition).isEmpty());
    }

    @Test
    void flagsAResourceAlreadyBookedByAnotherExpedition() {
        worker.reserve(new Reservation(ANOTHER_EXPEDITION, hours(10, 11)));
        scheduleWithWorker("a-1", hours(9, 11));

        final var expected = ValidationResult.critical(ResourceOverlapValidation.ID, ValidationSubject.of(new ActivityId("a-1"), new ResourceId("p-2")), "p-2 is not available during Sampling a-1");
        assertEquals(List.of(expected), validation.validate(expedition));
    }

    @Test
    void flagsAResourceThatIsUnavailable() {
        worker.markUnavailable(new Unavailability(hours(10, 11), "Medical leave"));
        scheduleWithWorker("a-1", hours(9, 11));

        final var expected = ValidationResult.critical(ResourceOverlapValidation.ID, ValidationSubject.of(new ActivityId("a-1"), new ResourceId("p-2")), "p-2 is not available during Sampling a-1");
        assertEquals(List.of(expected), validation.validate(expedition));
    }

    /**
     * The defect D59 fixed: the reservations an expedition's own approval made are its plan, not a competitor
     * for it. Before the fix, validating it again after approving it reported every assigned resource as
     * unavailable. The reservations are made the same way the approval makes them ({@code ApproveExpedition}).
     */
    @Test
    void acceptsTheResourcesTheExpeditionItselfReserved() {
        scheduleWithWorker("a-1", hours(9, 11));
        scheduleWithWorker("a-2", hours(11, 13));
        new ResourceReservations().reserveFor(expedition);

        assertTrue(validation.validate(expedition).isEmpty());
    }

    /**
     * The other side of D59: only its own reservations and the ones it takes over (D33) stop counting, so a
     * resource another expedition booked is still a critical.
     */
    @Test
    void flagsAResourceReservedByADifferentExpedition() {
        final var other = expeditionIn(coast, person("p-1")).build(repository);
        other.assignStaff(other.schedule(sampling("a-9", coast, hours(0, 24)), hours(10, 12)), worker);
        new ResourceReservations().reserveFor(other);
        scheduleWithWorker("a-1", hours(9, 11));

        final var expected = ValidationResult.critical(ResourceOverlapValidation.ID, ValidationSubject.of(new ActivityId("a-1"), new ResourceId("p-2")), "p-2 is not available during Sampling a-1");
        assertEquals(List.of(expected), validation.validate(expedition));
    }

    /**
     * An unavailability has no owner, so it is never one of the reservations D59 ignores: a resource that
     * falls out after the expedition reserved it (a medical leave, maintenance) is still a critical (D27).
     */
    @Test
    void flagsAResourceMarkedUnavailableOverItsOwnReservation() {
        scheduleWithWorker("a-1", hours(9, 11));
        new ResourceReservations().reserveFor(expedition);
        worker.markUnavailable(new Unavailability(hours(10, 11), "Medical leave"));

        final var expected = ValidationResult.critical(ResourceOverlapValidation.ID, ValidationSubject.of(new ActivityId("a-1"), new ResourceId("p-2")), "p-2 is not available during Sampling a-1");
        assertEquals(List.of(expected), validation.validate(expedition));
    }

    private PlannedNode planned(String activityId, TimePeriod period) {
        return activity(sampling(activityId, coast, hours(0, 24)), period);
    }

    private void assignWorkerTo(String... activityIds) {
        Stream.of(activityIds)
                .map(activityId -> expedition.getItinerary().itemFor(new ActivityId(activityId)).orElseThrow())
                .forEach(item -> expedition.assignStaff(item, worker));
    }

    @Test
    void detectsAResourceConflictBetweenParallelActivitiesOfTheSameBlock() {
        expedition.schedule(parallel(planned("a-1", hours(9, 11)), planned("a-2", hours(9, 11))));
        assignWorkerTo("a-1", "a-2");

        final var expected = ValidationResult.critical(ResourceOverlapValidation.ID, ValidationSubject.of(new ActivityId("a-1"), new ResourceId("p-2")), "p-2 is assigned to Sampling a-1 and Sampling a-2 at the same time");
        assertEquals(List.of(expected), validation.validate(expedition));
    }

    @Test
    void flagsAResourceSharedByParallelBranchesEvenIfTheirPeriodsDoNotOverlap() {
        expedition.schedule(parallel(planned("a-1", hours(9, 11)), planned("a-2", hours(13, 15))));
        assignWorkerTo("a-1", "a-2");

        final var expected = ValidationResult.critical(ResourceOverlapValidation.ID, ValidationSubject.of(new ActivityId("a-1"), new ResourceId("p-2")), "p-2 is assigned to Sampling a-1 and Sampling a-2, which run in parallel in the same block");
        assertEquals(List.of(expected), validation.validate(expedition));
    }

    @Test
    void flagsAResourceSharedAcrossTheBranchesOfAParallelBlockThatHoldsOtherBlocks() {
        expedition.schedule(parallel(
                sequential(planned("a-1", hours(9, 10)), planned("a-2", hours(10, 11))),
                planned("a-3", hours(13, 15))));
        assignWorkerTo("a-2", "a-3");

        final var expected = ValidationResult.critical(ResourceOverlapValidation.ID, ValidationSubject.of(new ActivityId("a-2"), new ResourceId("p-2")), "p-2 is assigned to Sampling a-2 and Sampling a-3, which run in parallel in the same block");
        assertEquals(List.of(expected), validation.validate(expedition));
    }

    @Test
    void acceptsAResourceSharedByConsecutiveActivitiesOfASequentialBlock() {
        expedition.schedule(sequential(planned("a-1", hours(9, 11)), planned("a-2", hours(11, 13))));
        assignWorkerTo("a-1", "a-2");

        assertTrue(validation.validate(expedition).isEmpty());
    }

    @Test
    void acceptsAResourceSharedByActivitiesOfTheSameBranchOfAParallelBlock() {
        expedition.schedule(parallel(
                sequential(planned("a-1", hours(9, 10)), planned("a-2", hours(10, 11))),
                planned("a-3", hours(9, 11))));
        assignWorkerTo("a-1", "a-2");

        assertTrue(validation.validate(expedition).isEmpty());
    }

    @Test
    void acceptsAResourceSharedByABlockAndALooseActivityThatDoNotOverlap() {
        expedition.schedule(parallel(planned("a-1", hours(9, 11)), planned("a-2", hours(9, 11))));
        scheduleWithWorker("a-3", hours(13, 15));
        assignWorkerTo("a-1");

        assertTrue(validation.validate(expedition).isEmpty());
    }
}
