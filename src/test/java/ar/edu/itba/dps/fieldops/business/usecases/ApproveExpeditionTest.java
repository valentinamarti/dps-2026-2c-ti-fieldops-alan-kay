package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.ExpeditionNotApprovableException;
import ar.edu.itba.dps.fieldops.business.exceptions.InsufficientStockException;
import ar.edu.itba.dps.fieldops.business.exceptions.ResourceUnavailableException;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.validation.AcceptedWarning;
import ar.edu.itba.dps.fieldops.business.models.validation.RuleId;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationSubject;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.services.ResourceReservations;
import ar.edu.itba.dps.fieldops.business.validations.ApprovalPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.EXPEDITION_ID;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.assignContainers;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.assignCooler;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.diving;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.supply;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApproveExpeditionTest {

    private static final ValidationResult MISSING_PERMIT = ValidationResult.critical(new RuleId("missing-permits"), ValidationSubject.expedition(), "Zone z-1 requires a permit");
    private static final ValidationResult RISKY_DIVE = ValidationResult.warning(new RuleId("risk-tolerance"), ValidationSubject.of(new ActivityId("a-1")), "Diving a-1 has HIGH risk");

    private final Zone coast = zone("z-1");
    private final Person leader = person("p-1");
    private final Expedition expedition = expeditionIn(coast, leader).build(EXPEDITION_ID);
    private final ResourceReservations reservations = new ResourceReservations();

    private ApproveExpedition finding(ValidationResult... results) {
        return new ApproveExpedition(new ApprovalPolicy(List.of(_ -> List.of(results))), reservations);
    }

    @Test
    void reservesTheAssignedResourcesForTheirScheduledPeriods() {
        final var item = expedition.schedule(sampling("a-1", coast, hours(8, 18)), hours(9, 11));
        final var worker = person("p-2");
        expedition.assignStaff(item, worker);
        assignCooler(expedition, item);
        assignContainers(expedition, item);
        expedition.submitForReview();

        finding().approve(expedition, List.of());

        assertEquals(ExpeditionStatus.APPROVED, expedition.getStatus());
        assertFalse(worker.isAvailableDuring(hours(10, 12)));
        assertTrue(worker.isAvailableDuring(hours(11, 13)));
    }

    @Test
    void anExpeditionThatDoesNotPassTheValidationReservesNothing() {
        final var item = expedition.schedule(sampling("a-1", coast, hours(8, 18)), hours(9, 11));
        final var worker = person("p-2");
        expedition.assignStaff(item, worker);
        expedition.submitForReview();

        assertThrows(ExpeditionNotApprovableException.class, () -> finding(MISSING_PERMIT).approve(expedition, List.of()));

        assertEquals(ExpeditionStatus.IN_REVIEW, expedition.getStatus());
        assertTrue(worker.isAvailableDuring(hours(9, 11)));
    }

    @Test
    void aResourceTakenAfterTheValidationStopsTheApprovalWithoutReservingAnything() {
        final var takenWorker = person("p-2");
        final var freeWorker = person("p-3");
        final var otherExpedition = expeditionIn(coast, leader).build(EXPEDITION_ID);
        final var otherItem = otherExpedition.schedule(sampling("a-9", coast, hours(8, 18)), hours(9, 11));
        otherExpedition.assignStaff(otherItem, takenWorker);
        assignCooler(otherExpedition, otherItem);
        assignContainers(otherExpedition, otherItem);
        otherExpedition.submitForReview();
        finding().approve(otherExpedition, List.of());

        final var earlyItem = expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(6, 8));
        final var clashingItem = expedition.schedule(sampling("a-2", coast, hours(0, 24)), hours(10, 12));
        expedition.assignStaff(earlyItem, freeWorker);
        expedition.assignStaff(clashingItem, takenWorker);
        assignCooler(expedition, earlyItem);
        assignContainers(expedition, earlyItem);
        assignCooler(expedition, clashingItem);
        assignContainers(expedition, clashingItem);
        expedition.submitForReview();
        final var acceptances = List.of(new AcceptedWarning(RISKY_DIVE, leader, "Both divers are instructors"));

        assertThrows(ResourceUnavailableException.class, () -> finding(RISKY_DIVE).approve(expedition, acceptances));
        assertEquals(ExpeditionStatus.IN_REVIEW, expedition.getStatus());
        assertTrue(expedition.getAcceptedWarnings().isEmpty());
        assertTrue(freeWorker.isAvailableDuring(hours(6, 8)));
    }

    @Test
    void aDiveWithoutDiversIsNotApprovedEvenWithoutAdditionalRules() {
        expedition.schedule(diving("a-1", coast, hours(0, 24), 30), hours(9, 11));
        expedition.submitForReview();
        final var approveExpedition = new ApproveExpedition(new ApprovalPolicy(List.of()), reservations);

        assertThrows(ExpeditionNotApprovableException.class, () -> approveExpedition.approve(expedition, List.of()));
        assertEquals(ExpeditionStatus.IN_REVIEW, expedition.getStatus());
    }

    @Test
    void aFailedApprovalReservesNothing() {
        final var diver = person("p-2");
        final var first = expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(9, 11));
        final var second = expedition.schedule(sampling("a-2", coast, hours(0, 24)), hours(10, 12));
        expedition.assignStaff(first, diver);
        expedition.assignStaff(second, diver);
        expedition.submitForReview();

        assertThrows(ResourceUnavailableException.class, () -> finding().approve(expedition, List.of()));

        assertEquals(ExpeditionStatus.IN_REVIEW, expedition.getStatus());
        assertTrue(diver.isAvailableDuring(hours(9, 11)));
    }

    @Test
    void aSecondExpeditionCannotBeApprovedWithStockAlreadyReserved() {
        final var jars = supply("d-1", "Sample container", Quantity.of("3", UNITS));
        final var policy = new ApprovalPolicy(List.of());
        final var approveExpedition = new ApproveExpedition(policy, reservations);
        final var first = expeditionIn(coast, leader).build(new ExpeditionId("e-1"));
        final var firstItem = first.schedule(sampling("a-1", coast, hours(0, 24)), hours(9, 11));
        first.assignSupply(firstItem, jars);
        first.assignStaff(firstItem, person("p-2"));
        assignCooler(first, firstItem);
        final var second = expeditionIn(coast, leader).build(new ExpeditionId("e-2"));
        final var secondItem = second.schedule(sampling("a-2", coast, hours(0, 24)), hours(13, 15));
        second.assignSupply(secondItem, jars);
        second.assignStaff(secondItem, person("p-3"));
        assignCooler(second, secondItem);
        first.submitForReview();
        approveExpedition.approve(first, List.of());
        second.submitForReview();

        assertFalse(policy.validate(second).canBeApproved());
        assertThrows(InsufficientStockException.class, () -> approveExpedition.approve(second, List.of()));
        assertEquals(ExpeditionStatus.IN_REVIEW, second.getStatus());
        assertEquals(Quantity.of("3", UNITS), jars.getStock());
    }
}
