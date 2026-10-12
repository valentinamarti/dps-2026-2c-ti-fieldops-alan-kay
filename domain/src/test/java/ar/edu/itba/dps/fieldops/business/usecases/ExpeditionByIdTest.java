package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.estimations.ExpeditionEstimator;
import ar.edu.itba.dps.fieldops.business.exceptions.NotFoundException;
import ar.edu.itba.dps.fieldops.business.exceptions.UnacceptedWarningException;
import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Observation;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.validation.RuleId;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationSubject;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.replanning.Cancellation;
import ar.edu.itba.dps.fieldops.business.replanning.ExpeditionReplanner;
import ar.edu.itba.dps.fieldops.business.reports.ExpeditionReporter;
import ar.edu.itba.dps.fieldops.business.services.ResourceReservations;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.completeAssignments;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The use cases that receive an id instead of the expedition: queries, tracking and the id versions of approve, finish and replan.
 */
class ExpeditionByIdTest {

    private static final ValidationResult RISKY = ValidationResult.warning(
            new RuleId("risk-tolerance"), ValidationSubject.of(new ActivityId("a-1")), "Sampling a-1 has HIGH risk");

    private final SequentialExpeditionRepository stored = new SequentialExpeditionRepository();
    private final ExpeditionRepository repository = mock(ExpeditionRepository.class);
    private final ResourceCatalog catalog = new ResourceCatalog();
    private final ResourceReservations reservations = new ResourceReservations();
    private final Zone coast = zone("z-1");
    private final Person leader = person("p-1");
    private final Expedition expedition = expeditionIn(coast, leader).build(stored);
    private final ExpeditionId unknown = new ExpeditionId("e-404");

    ExpeditionByIdTest() {
        when(repository.findById(expedition.getId())).thenReturn(Optional.of(expedition));
        when(repository.findById(unknown)).thenReturn(Optional.empty());
        when(repository.findAll()).thenReturn(List.of(expedition));
    }

    private void readyToApprove() {
        expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(9, 11));
        completeAssignments(expedition);
        expedition.submitForReview();
    }

    @Test
    void getsAnExpeditionByIdAndListsThemAll() {
        final var queries = new QueryExpeditions(repository);

        assertSame(expedition, queries.get(expedition.getId()));
        assertEquals(List.of(expedition), queries.list());
        assertThrows(NotFoundException.class, () -> queries.get(unknown));
    }

    @Test
    void validatesWithoutApprovingAndShowsWhatBlocksIt() {
        readyToApprove();
        final var blockedByARule = new ValidateExpedition(repository,
                List.of(_ -> List.of(ValidationResult.critical(new RuleId("custom"), ValidationSubject.expedition(), "Blocked"))));

        final var result = blockedByARule.validate(expedition.getId());

        assertFalse(result.canBeApproved());
        assertEquals(ExpeditionStatus.IN_REVIEW, expedition.getStatus());
        assertThrows(NotFoundException.class, () -> blockedByARule.validate(unknown));
    }

    @Test
    void estimatesAndReportsByExpeditionId() {
        readyToApprove();
        final var estimator = new ExpeditionEstimator();
        final var estimates = new EstimateExpedition(repository, estimator);
        final var reports = new ReportExpedition(repository, new ExpeditionReporter(estimator));

        assertEquals(estimator.estimate(expedition), estimates.estimate(expedition.getId()));
        assertEquals(new ExpeditionReporter(estimator).reportOf(expedition), reports.report(expedition.getId()));
        assertThrows(NotFoundException.class, () -> estimates.estimate(unknown));
        assertThrows(NotFoundException.class, () -> reports.report(unknown));
    }

    @Test
    void approvesByIdAcceptingTheWarningsTheCommandNames() {
        catalog.addStaff(leader);
        readyToApprove();
        final var approve = new ApproveExpedition(List.of(_ -> List.of(RISKY)), reservations, repository, catalog);
        final var acceptance = new WarningAcceptance(RISKY.rule(), RISKY.subject(), new ResourceId("p-1"), "Both divers are instructors");

        approve.approve(expedition.getId(), List.of(acceptance));

        assertEquals(ExpeditionStatus.APPROVED, expedition.getStatus());
        assertEquals(List.of(RISKY), expedition.getAcceptedWarnings().stream().map(accepted -> accepted.warning()).toList());
    }

    @Test
    void approvingByIdWithoutAcceptingAWarningFailsLikeApprovingTheObject() {
        readyToApprove();
        final var approve = new ApproveExpedition(List.of(_ -> List.of(RISKY)), reservations, repository, catalog);

        assertThrows(UnacceptedWarningException.class, () -> approve.approve(expedition.getId(), List.of()));
        assertEquals(ExpeditionStatus.IN_REVIEW, expedition.getStatus());
    }

    @Test
    void acceptingAWarningTheExpeditionDoesNotRaiseIsAnError() {
        catalog.addStaff(leader);
        readyToApprove();
        final var approve = new ApproveExpedition(List.of(), reservations, repository, catalog);
        final var stale = new WarningAcceptance(RISKY.rule(), RISKY.subject(), new ResourceId("p-1"), "Both divers are instructors");

        assertThrows(NotFoundException.class, () -> approve.approve(expedition.getId(), List.of(stale)));
        assertThrows(NotFoundException.class, () -> approve.approve(unknown, List.of()));
    }

    @Test
    void trackingAnActivityByIdStartsFinishesAndObserves() {
        readyToApprove();
        new ApproveExpedition(List.of(), reservations, repository, catalog).approve(expedition.getId(), List.of());
        expedition.start();
        final var tracking = new TrackActivity(repository);
        final var activity = new ActivityId("a-1");
        final var item = expedition.getItinerary().itemFor(activity).orElseThrow();
        final var at = LocalDateTime.of(2026, 10, 1, 9, 30);

        tracking.start(expedition.getId(), activity, at);
        tracking.observe(expedition.getId(), activity, new Observation("Calm water", at.plusMinutes(10)));
        tracking.finish(expedition.getId(), activity, at.plusHours(1), "Six samples");

        assertTrue(item.isFinished());
        assertEquals(1, item.getTracking().orElseThrow().getObservations().size());
        assertThrows(NotFoundException.class, () -> tracking.start(unknown, activity, at));
        assertThrows(NotFoundException.class, () -> tracking.start(expedition.getId(), new ActivityId("a-404"), at));
    }

    @Test
    void finishesByIdFreeingTheReservations() {
        readyToApprove();
        new ApproveExpedition(List.of(), reservations, repository, catalog).approve(expedition.getId(), List.of());
        expedition.start();
        final var finish = new FinishExpedition(reservations, repository);

        finish.finish(expedition.getId());

        assertEquals(ExpeditionStatus.FINISHED, expedition.getStatus());
        assertThrows(NotFoundException.class, () -> finish.finish(unknown));
    }

    @Test
    void replansByIdTheActivityTheCommandNames() {
        expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(9, 11));
        final var affected = expedition.schedule(sampling("a-2", coast, hours(0, 24)), hours(12, 14));
        completeAssignments(expedition);
        expedition.submitForReview();
        expedition.approve(List.of(), List.of());
        final var replan = new ReplanExpedition(repository, new ExpeditionReplanner(stored));

        final var alternative = replan.replan(expedition.getId(), affected.getActivity().getId(), new Cancellation());

        assertEquals(ExpeditionStatus.DRAFT, alternative.getStatus());
        assertEquals(Optional.of(expedition.getId()), alternative.getReplacedExpedition());
        assertThrows(NotFoundException.class, () -> replan.replan(unknown, new ActivityId("a-2"), new Cancellation()));
        assertThrows(NotFoundException.class, () -> replan.replan(expedition.getId(), new ActivityId("a-404"), new Cancellation()));
    }
}
