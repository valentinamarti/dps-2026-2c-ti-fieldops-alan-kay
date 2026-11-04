package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.exceptions.ExpeditionNotApprovableException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidStatusTransitionException;
import ar.edu.itba.dps.fieldops.business.exceptions.UnacceptedWarningException;
import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.interfaces.validation.ValidationRule;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.events.ExpeditionApproved;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.validation.AcceptedWarning;
import ar.edu.itba.dps.fieldops.business.models.validation.RuleId;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationSubject;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.APPROVED_AT;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.diving;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExpeditionApprovalTest {

    private static final ValidationResult MISSING_PERMIT = ValidationResult.critical(new RuleId("missing-permits"), ValidationSubject.expedition(), "Zone z-1 requires a permit");
    private static final ValidationResult RISKY_DIVE = ValidationResult.warning(new RuleId("risk-tolerance"), ValidationSubject.of(new ActivityId("a-1")), "Diving a-1 has HIGH risk");

    private final SequentialExpeditionRepository repository = new SequentialExpeditionRepository();
    private final Zone coast = zone("z-1");
    private final Person leader = person("p-1");
    private final Expedition expedition = expeditionIn(coast, leader).build(repository);

    private static List<ValidationRule> finding(ValidationResult... results) {
        return List.of(_ -> List.of(results));
    }

    @Test
    void approvesAnExpeditionInReviewWithoutProblems() {
        expedition.submitForReview();

        expedition.approve(finding(), List.of(), APPROVED_AT);

        assertEquals(ExpeditionStatus.APPROVED, expedition.getStatus());
    }

    @Test
    void approvingRecordsExpeditionApprovedAtTheMomentItReceives() {
        expedition.submitForReview();

        expedition.approve(finding(), List.of(), APPROVED_AT);

        final var events = expedition.pullEvents();
        assertEquals(1, events.size());
        final var event = assertInstanceOf(ExpeditionApproved.class, events.getFirst());
        assertEquals(expedition.getId(), event.expeditionId());
        assertEquals(APPROVED_AT, event.occurredAt());
    }

    @Test
    void anApprovalThatIsRejectedRecordsNothing() {
        expedition.submitForReview();

        assertThrows(ExpeditionNotApprovableException.class, () -> expedition.approve(finding(MISSING_PERMIT), List.of(), APPROVED_AT));
        assertThrows(UnacceptedWarningException.class, () -> expedition.approve(finding(RISKY_DIVE), List.of(), APPROVED_AT));

        assertEquals(List.of(), expedition.pullEvents());
    }

    @Test
    void anApprovalNeedsTheMomentItHappened() {
        expedition.submitForReview();

        assertThrows(NullPointerException.class, () -> expedition.approve(finding(), List.of(), null));
        assertEquals(ExpeditionStatus.IN_REVIEW, expedition.getStatus());
    }

    @Test
    void theMandatoryRulesRunEvenWithoutAdditionalRules() {
        expedition.schedule(diving("a-1", coast, hours(0, 24), 30), hours(9, 11));
        expedition.submitForReview();

        assertThrows(ExpeditionNotApprovableException.class, () -> expedition.approve(List.of(), List.of(), APPROVED_AT));
        assertEquals(ExpeditionStatus.IN_REVIEW, expedition.getStatus());
    }

    @Test
    void cannotBeApprovedWithCriticalProblems() {
        final var acceptance = new AcceptedWarning(RISKY_DIVE, leader, "Both divers are instructors");
        expedition.submitForReview();

        assertThrows(ExpeditionNotApprovableException.class,
                () -> expedition.approve(finding(MISSING_PERMIT, RISKY_DIVE), List.of(acceptance), APPROVED_AT));
        assertEquals(ExpeditionStatus.IN_REVIEW, expedition.getStatus());
    }

    @Test
    void cannotBeApprovedWithAWarningNobodyAccepted() {
        expedition.submitForReview();

        assertThrows(UnacceptedWarningException.class, () -> expedition.approve(finding(RISKY_DIVE), List.of(), APPROVED_AT));
        assertEquals(ExpeditionStatus.IN_REVIEW, expedition.getStatus());
    }

    @Test
    void approvesWhenAResponsibleAcceptedEveryWarningAndRecordsTheJustification() {
        final var acceptance = new AcceptedWarning(RISKY_DIVE, leader, "Both divers are instructors");
        expedition.submitForReview();

        expedition.approve(finding(RISKY_DIVE), List.of(acceptance), APPROVED_AT);

        assertEquals(ExpeditionStatus.APPROVED, expedition.getStatus());
        assertEquals(List.of(acceptance), expedition.getAcceptedWarnings());
    }

    @Test
    void aWarningAcceptedBySomeoneWhoIsNotResponsibleDoesNotCount() {
        final var outsiderAcceptance = new AcceptedWarning(RISKY_DIVE, person("p-9"), "Looks fine to me");
        expedition.submitForReview();

        assertThrows(UnacceptedWarningException.class,
                () -> expedition.approve(finding(RISKY_DIVE), List.of(outsiderAcceptance), APPROVED_AT));
    }

    @Test
    void mustBeInReviewToBeApproved() {
        assertThrows(InvalidStatusTransitionException.class, () -> expedition.approve(finding(), List.of(), APPROVED_AT));
        assertEquals(ExpeditionStatus.DRAFT, expedition.getStatus());
    }

    @Test
    void anAcceptanceStillCoversItsWarningWhenTheWordingChanges() {
        final var acceptance = new AcceptedWarning(RISKY_DIVE, leader, "Both divers are instructors");
        final var reworded = ValidationResult.warning(RISKY_DIVE.rule(), RISKY_DIVE.subject(), "Diving a-1 is riskier than allowed");
        expedition.submitForReview();

        expedition.approve(finding(reworded), List.of(acceptance), APPROVED_AT);

        assertEquals(ExpeditionStatus.APPROVED, expedition.getStatus());
    }

    @Test
    void anAcceptanceForAnotherActivityDoesNotCoverTheWarning() {
        final var otherDive = ValidationResult.warning(RISKY_DIVE.rule(), ValidationSubject.of(new ActivityId("a-2")), "Diving a-2 has HIGH risk");
        final var acceptance = new AcceptedWarning(otherDive, leader, "Both divers are instructors");
        expedition.submitForReview();

        assertThrows(UnacceptedWarningException.class, () -> expedition.approve(finding(RISKY_DIVE), List.of(acceptance), APPROVED_AT));
    }

    @Test
    void onlyWarningsCanBeAcceptedAndOnlyWithAJustification() {
        assertThrows(IllegalArgumentException.class, () -> new AcceptedWarning(MISSING_PERMIT, leader, "We will get it later"));
        assertThrows(IllegalArgumentException.class, () -> new AcceptedWarning(RISKY_DIVE, leader, " "));
    }
}