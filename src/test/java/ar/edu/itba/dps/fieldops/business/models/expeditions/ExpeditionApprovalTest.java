package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.exceptions.ExpeditionNotApprovableException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidStatusTransitionException;
import ar.edu.itba.dps.fieldops.business.exceptions.UnacceptedWarningException;
import ar.edu.itba.dps.fieldops.business.interfaces.validation.ExpeditionValidator;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.validation.AcceptedWarning;
import ar.edu.itba.dps.fieldops.business.models.validation.ApprovalResult;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExpeditionApprovalTest {

    private static final ValidationResult MISSING_PERMIT = ValidationResult.critical("Zone z-1 requires a permit");
    private static final ValidationResult RISKY_DIVE = ValidationResult.warning("Diving a-1 has HIGH risk");

    private final Zone coast = zone("z-1");
    private final Person leader = person("p-1");
    private final Expedition expedition = expeditionIn(coast, leader).build();

    private static ExpeditionValidator finding(ValidationResult... results) {
        return expedition -> new ApprovalResult(List.of(results));
    }

    @Test
    void approvesAnExpeditionInReviewWithoutProblems() {
        expedition.submitForReview();

        expedition.approve(finding(), List.of());

        assertEquals(ExpeditionStatus.APPROVED, expedition.getStatus());
    }

    @Test
    void cannotBeApprovedWithCriticalProblems() {
        final var acceptance = new AcceptedWarning(RISKY_DIVE, leader, "Both divers are instructors");
        expedition.submitForReview();

        assertThrows(ExpeditionNotApprovableException.class,
                () -> expedition.approve(finding(MISSING_PERMIT, RISKY_DIVE), List.of(acceptance)));
        assertEquals(ExpeditionStatus.IN_REVIEW, expedition.getStatus());
    }

    @Test
    void cannotBeApprovedWithAWarningNobodyAccepted() {
        expedition.submitForReview();

        assertThrows(UnacceptedWarningException.class, () -> expedition.approve(finding(RISKY_DIVE), List.of()));
        assertEquals(ExpeditionStatus.IN_REVIEW, expedition.getStatus());
    }

    @Test
    void approvesWhenAResponsibleAcceptedEveryWarningAndRecordsTheJustification() {
        final var acceptance = new AcceptedWarning(RISKY_DIVE, leader, "Both divers are instructors");
        expedition.submitForReview();

        expedition.approve(finding(RISKY_DIVE), List.of(acceptance));

        assertEquals(ExpeditionStatus.APPROVED, expedition.getStatus());
        assertEquals(List.of(acceptance), expedition.getAcceptedWarnings());
    }

    @Test
    void aWarningAcceptedBySomeoneWhoIsNotResponsibleDoesNotCount() {
        final var outsiderAcceptance = new AcceptedWarning(RISKY_DIVE, person("p-9"), "Looks fine to me");
        expedition.submitForReview();

        assertThrows(UnacceptedWarningException.class,
                () -> expedition.approve(finding(RISKY_DIVE), List.of(outsiderAcceptance)));
    }

    @Test
    void mustBeInReviewToBeApproved() {
        assertThrows(InvalidStatusTransitionException.class, () -> expedition.approve(finding(), List.of()));
        assertEquals(ExpeditionStatus.DRAFT, expedition.getStatus());
    }

    @Test
    void onlyWarningsCanBeAcceptedAndOnlyWithAJustification() {
        assertThrows(IllegalArgumentException.class, () -> new AcceptedWarning(MISSING_PERMIT, leader, "We will get it later"));
        assertThrows(IllegalArgumentException.class, () -> new AcceptedWarning(RISKY_DIVE, leader, " "));
    }
}