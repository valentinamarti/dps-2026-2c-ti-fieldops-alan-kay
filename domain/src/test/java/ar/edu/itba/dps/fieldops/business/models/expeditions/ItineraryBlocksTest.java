package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.estimations.ExpeditionEstimator;
import ar.edu.itba.dps.fieldops.business.exceptions.ExpeditionNotEditableException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidScheduleException;
import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.interfaces.itinerary.ItineraryNode;
import ar.edu.itba.dps.fieldops.business.interfaces.itinerary.ItineraryNodeVisitor;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationSubject;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.validations.ApprovalPolicy;
import ar.edu.itba.dps.fieldops.business.validations.MissingCertificationsValidation;
import ar.edu.itba.dps.fieldops.business.validations.MissingPermitsValidation;
import ar.edu.itba.dps.fieldops.business.validations.MissingResourcesValidation;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.completeAssignments;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.nightDiving;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.activity;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.parallel;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.sequential;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItineraryBlocksTest {
    private static final ItineraryNodeVisitor<String> SHAPE = new ItineraryNodeVisitor<>() {
        @Override
        public String activity(ItineraryItem item) {
            return item.getActivity().getId().value();
        }

        @Override
        public String sequence(ItineraryNode block, List<String> parts) {
            return "sequential(%s)".formatted(String.join(", ", parts));
        }

        @Override
        public String parallel(ItineraryNode block, List<String> branches) {
            return "parallel(%s)".formatted(String.join(", ", branches));
        }
    };

    private final SequentialExpeditionRepository repository = new SequentialExpeditionRepository();
    private final Zone coast = zone("z-1");
    private final Person leader = person("p-1");
    private final Expedition expedition = expeditionIn(coast, leader).build(repository);
    private final Activity landing = survey("a-1");
    private final Activity dive = survey("a-2");
    private final Activity transect = survey("a-3");
    private final Activity wrapUp = survey("a-4");

    private Activity survey(String id, Activity... dependencies) {
        return sampling(id, coast, hours(0, 24), dependencies);
    }

    private ItineraryItem itemOf(Activity activity) {
        return expedition.getItinerary().itemFor(activity).orElseThrow();
    }

    private static LocalDateTime at(int hour) {
        return LocalDateTime.of(2026, 10, 1, hour, 0);
    }

    @Test
    void anItineraryCanHaveAParallelBlockInsideASequentialBlock() {
        final var block = expedition.schedule(sequential(
                activity(landing, hours(8, 9)),
                parallel(
                        activity(dive, hours(9, 11)),
                        activity(transect, hours(9, 10))),
                activity(wrapUp, hours(11, 12))));

        final var itinerary = expedition.getItinerary();
        assertEquals(List.of(block), itinerary.getNodes());
        assertEquals(List.of(itemOf(landing), itemOf(dive), itemOf(transect), itemOf(wrapUp)), itinerary.getItems());
        assertTrue(itinerary.runInParallel(itemOf(dive), itemOf(transect)));
        assertFalse(itinerary.runInParallel(itemOf(landing), itemOf(dive)));
        assertFalse(itinerary.runInParallel(itemOf(transect), itemOf(wrapUp)));
    }

    @Test
    void aVisitorReadsTheShapeOfTheTreeWithoutAnyoneAskingWhatKindEachNodeIs() {
        final var block = expedition.schedule(sequential(
                activity(landing, hours(8, 9)),
                parallel(
                        activity(dive, hours(9, 11)),
                        activity(transect, hours(9, 10)))));

        assertEquals("sequential(a-1, parallel(a-2, a-3))", block.accept(SHAPE));
        assertEquals("a-1", itemOf(landing).accept(SHAPE));
    }

    @Test
    void thePeriodOfABlockGoesFromItsEarliestStartToItsLatestEnd() {
        final var block = expedition.schedule(parallel(
                activity(dive, hours(10, 13)),
                activity(transect, hours(8, 11))));

        assertEquals(hours(8, 13), block.getScheduledPeriod());
    }

    @Test
    void theItemsOfBlocksAndLooseActivitiesAreListedTogetherByStartTime() {
        final var loose = expedition.schedule(landing, hours(10, 12));
        expedition.schedule(parallel(
                activity(dive, hours(13, 14)),
                activity(transect, hours(8, 9))));

        assertEquals(List.of(itemOf(transect), loose, itemOf(dive)), expedition.getItinerary().getItems());
        assertEquals(2, expedition.getItinerary().getNodes().size());
    }

    @Test
    void theActivitiesOfABlockGetResourcesLikeAnyOtherActivity() {
        expedition.schedule(parallel(activity(dive, hours(9, 11)), activity(transect, hours(9, 11))));
        final var worker = person("p-2");

        expedition.assignStaff(itemOf(dive), worker);

        assertEquals(Set.of(worker), itemOf(dive).getAssignedStaff());
    }

    @Test
    void aBlockIsFinishedOnceEveryActivityInsideIsFinished() {
        final var block = expedition.schedule(sequential(activity(dive, hours(9, 11)), activity(transect, hours(11, 13))));
        completeAssignments(expedition);
        expedition.submitForReview();
        expedition.approve(List.of(), List.of());
        expedition.start();

        expedition.startActivity(itemOf(dive), at(9));
        expedition.finishActivity(itemOf(dive), at(11), "Done");
        assertFalse(block.isFinished());

        expedition.startActivity(itemOf(transect), at(11));
        expedition.finishActivity(itemOf(transect), at(13), "Done");
        assertTrue(block.isFinished());
    }

    @Test
    void rejectsAnEmptyBlock() {
        assertThrows(InvalidScheduleException.class, () -> expedition.schedule(sequential()));
        assertThrows(InvalidScheduleException.class, () -> expedition.schedule(parallel()));
        assertThrows(InvalidScheduleException.class,
                () -> expedition.schedule(sequential(activity(landing, hours(8, 9)), parallel())));
    }

    @Test
    void rejectsABlockWithAChildOutsideTheExpeditionPeriod() {
        final var nightShift = sampling("a-9", coast, hours(20, 30));

        assertThrows(InvalidScheduleException.class, () -> expedition.schedule(parallel(
                activity(dive, hours(9, 11)),
                activity(nightShift, hours(23, 25)))));
    }

    @Test
    void rejectsABlockWithAnActivityOfAZoneThatIsNotPartOfTheExpedition() {
        final var elsewhere = sampling("a-9", zone("z-2"), hours(0, 24));

        assertThrows(InvalidScheduleException.class,
                () -> expedition.schedule(sequential(activity(dive, hours(9, 11)), activity(elsewhere, hours(11, 13)))));
    }

    @Test
    void rejectsASequentialBlockWhoseChildrenOverlap() {
        assertThrows(InvalidScheduleException.class, () -> expedition.schedule(sequential(
                activity(dive, hours(9, 11)),
                activity(transect, hours(10, 12)))));
    }

    @Test
    void rejectsASequentialBlockWhoseChildrenAreNotInTheOrderTheyRun() {
        assertThrows(InvalidScheduleException.class, () -> expedition.schedule(sequential(
                activity(dive, hours(11, 13)),
                activity(transect, hours(9, 10)))));
    }

    @Test
    void aBlockInsideASequentialBlockTakesItsTurnWithItsWholePeriod() {
        assertThrows(InvalidScheduleException.class, () -> expedition.schedule(sequential(
                parallel(
                        activity(dive, hours(9, 11)),
                        activity(transect, hours(9, 12))),
                activity(wrapUp, hours(11, 13)))));
    }

    @Test
    void aSequentialBlockAcceptsChildrenThatStartRightAfterOrLaterThanThePreviousOneEnds() {
        assertDoesNotThrow(() -> expedition.schedule(sequential(
                activity(landing, hours(8, 9)),
                activity(dive, hours(9, 11)),
                activity(transect, hours(14, 16)))));
    }

    @Test
    void aParallelBlockAcceptsBranchesWhosePeriodsDoNotOverlap() {
        assertDoesNotThrow(() -> expedition.schedule(parallel(
                activity(dive, hours(9, 11)),
                activity(transect, hours(14, 16)))));
    }

    @Test
    void rejectsABlockWithAnActivityThatIsAlreadyScheduled() {
        expedition.schedule(landing, hours(8, 9));

        assertThrows(InvalidScheduleException.class,
                () -> expedition.schedule(parallel(activity(landing, hours(10, 11)), activity(dive, hours(10, 11)))));
        assertThrows(InvalidScheduleException.class,
                () -> expedition.schedule(parallel(activity(dive, hours(10, 11)), activity(dive, hours(12, 13)))));
    }

    @Test
    void aRejectedBlockLeavesTheItineraryUntouched() {
        final var loose = expedition.schedule(landing, hours(8, 9));

        assertThrows(InvalidScheduleException.class, () -> expedition.schedule(sequential(
                activity(dive, hours(9, 11)),
                activity(transect, hours(10, 12)))));
        assertEquals(List.of(loose), expedition.getItinerary().getItems());
        assertEquals(List.of(loose), expedition.getItinerary().getNodes());
    }

    @Test
    void anActivityOfABlockCanDependOnAnEarlierOneOfTheSameBlockOrOnOneAlreadyScheduled() {
        expedition.schedule(landing, hours(8, 9));
        final var afterLanding = survey("a-5", landing);
        final var afterBoth = survey("a-6", landing, afterLanding);

        expedition.schedule(sequential(
                activity(afterLanding, hours(9, 11)),
                activity(afterBoth, hours(11, 13))));

        assertTrue(expedition.getItinerary().itemFor(afterBoth).isPresent());
    }

    @Test
    void rejectsABlockWithAnActivityWhoseDependencyIsNotScheduled() {
        final var afterLanding = survey("a-5", landing);

        assertThrows(InvalidScheduleException.class,
                () -> expedition.schedule(sequential(activity(dive, hours(9, 11)), activity(afterLanding, hours(11, 13)))));
    }

    @Test
    void rejectsABlockWithAnActivityThatStartsBeforeItsDependencyEnds() {
        expedition.schedule(landing, hours(10, 12));
        final var afterLanding = survey("a-5", landing);

        assertThrows(InvalidScheduleException.class,
                () -> expedition.schedule(parallel(activity(dive, hours(9, 11)), activity(afterLanding, hours(11, 13)))));
    }

    @Test
    void rejectsADependencyBetweenParallelBranchesEvenIfTheirPeriodsDoNotOverlap() {
        final var afterDive = survey("a-5", dive);

        assertThrows(InvalidScheduleException.class, () -> expedition.schedule(parallel(
                activity(dive, hours(9, 11)),
                activity(afterDive, hours(11, 13)))));
        assertThrows(InvalidScheduleException.class, () -> expedition.schedule(parallel(
                sequential(activity(landing, hours(8, 9)), activity(dive, hours(9, 11))),
                activity(afterDive, hours(11, 13)))));
    }

    @Test
    void aBlockCannotBeScheduledOnceTheExpeditionLeftTheDraft() {
        expedition.submitForReview();

        assertThrows(ExpeditionNotEditableException.class,
                () -> expedition.schedule(parallel(activity(dive, hours(9, 11)), activity(transect, hours(9, 11)))));
    }

    private static List<ValidationResult> findingsAbout(Expedition planned, Activity activity) {
        return new ApprovalPolicy(List.of()).validate(planned).results().stream()
                .filter(result -> result.subject().activity().equals(Optional.of(activity.getId())))
                .toList();
    }

    @Test
    void aNightActivityInsideABlockIsValidatedAndEstimatedLikeALooseOne() {
        final var nightDive = nightDiving("a-5", coast, hours(0, 24), 40);
        final var uncertified = person("p-2");
        final var loose = expeditionIn(coast, leader).build(repository);
        loose.assignStaff(loose.schedule(nightDive, hours(20, 23)), uncertified);

        final var block = expedition.schedule(sequential(
                activity(landing, hours(18, 20)),
                parallel(
                        activity(nightDive, hours(20, 23)),
                        activity(transect, hours(20, 22)))));
        expedition.assignStaff(itemOf(nightDive), uncertified);

        final var findings = findingsAbout(expedition, nightDive);
        assertEquals(findingsAbout(loose, nightDive), findings);
        assertTrue(findings.contains(ValidationResult.critical(MissingCertificationsValidation.ID, ValidationSubject.of(nightDive.getId(), uncertified.getId()), "Night diving a-5 requires everyone assigned to be certified in night operations, but p-2 is not")));
        assertTrue(findings.contains(ValidationResult.critical(MissingPermitsValidation.ID, ValidationSubject.of(nightDive.getId()), "Night diving a-5 requires the permit night operations in zone z-1, which was not granted")));
        assertTrue(findings.contains(ValidationResult.critical(MissingResourcesValidation.ID, ValidationSubject.of(nightDive.getId()), "Night diving a-5 has 0 of the 1 lighting it needs")));
        assertEquals(RiskLevel.EXTREME, block.estimatedRisk());
        assertEquals(new ExpeditionEstimator().estimate(loose).overallRisk(), new ExpeditionEstimator().estimate(expedition).overallRisk());
        assertEquals(Duration.ofMinutes(120 + 125), block.estimatedDuration());
    }
}
