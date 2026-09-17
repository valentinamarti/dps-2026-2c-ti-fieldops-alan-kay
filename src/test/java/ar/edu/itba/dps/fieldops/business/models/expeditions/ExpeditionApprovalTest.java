package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.exceptions.ExpeditionNotApprovableException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidStatusTransitionException;
import ar.edu.itba.dps.fieldops.business.exceptions.ResourceUnavailableException;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void reservesTheAssignedResourcesForTheirScheduledPeriods() {
        final var item = expedition.schedule(sampling("a-1", coast, hours(8, 18)), hours(9, 11));
        final var worker = person("p-2");
        expedition.assignStaff(item, worker);
        expedition.submitForReview();

        expedition.approve(finding(), List.of());

        assertFalse(worker.isAvailableDuring(hours(10, 12)));
        assertTrue(worker.isAvailableDuring(hours(11, 13)));
    }

    @Test
    void aResourceTakenAfterTheValidationStopsTheApprovalWithoutReservingAnything() {
        final var takenWorker = person("p-2");
        final var freeWorker = person("p-3");
        final var otherExpedition = expeditionIn(coast, leader).build();
        final var otherItem = otherExpedition.schedule(sampling("a-9", coast, hours(8, 18)), hours(9, 11));
        otherExpedition.assignStaff(otherItem, takenWorker);
        otherExpedition.submitForReview();
        otherExpedition.approve(finding(), List.of());

        final var earlyItem = expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(6, 8));
        final var clashingItem = expedition.schedule(sampling("a-2", coast, hours(0, 24)), hours(10, 12));
        expedition.assignStaff(earlyItem, freeWorker);
        expedition.assignStaff(clashingItem, takenWorker);
        expedition.submitForReview();

        assertThrows(ResourceUnavailableException.class, () -> expedition.approve(finding(RISKY_DIVE), acceptances()));
        assertEquals(ExpeditionStatus.IN_REVIEW, expedition.getStatus());
        assertTrue(expedition.getAcceptedWarnings().isEmpty());
        assertTrue(freeWorker.isAvailableDuring(hours(6, 8)));
    }

    private List<AcceptedWarning> acceptances() {
        return List.of(new AcceptedWarning(RISKY_DIVE, leader, "Both divers are instructors"));
    }

    @Test
    void onlyWarningsCanBeAcceptedAndOnlyWithAJustification() {
        assertThrows(IllegalArgumentException.class, () -> new AcceptedWarning(MISSING_PERMIT, leader, "We will get it later"));
        assertThrows(IllegalArgumentException.class, () -> new AcceptedWarning(RISKY_DIVE, leader, " "));
    }
}