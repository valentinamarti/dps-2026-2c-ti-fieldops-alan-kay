package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.ActivitiesInProgressException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidStatusTransitionException;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.services.ResourceReservations;
import ar.edu.itba.dps.fieldops.business.validations.ApprovalPolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.EXPEDITION_ID;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.assignContainers;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.assignCooler;
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

class FinishExpeditionTest {

    private final ResourceReservations reservations = new ResourceReservations();
    private final FinishExpedition finishExpedition = new FinishExpedition(reservations);
    private final Zone coast = zone("z-1");
    private final Expedition expedition = expeditionIn(coast, person("p-1")).build(EXPEDITION_ID);
    private final Person worker = person("p-2");

    private void approveEquipped() {
        expedition.getItinerary().getItems().forEach(item -> {
            expedition.assignStaff(item, worker);
            assignCooler(expedition, item);
            assignContainers(expedition, item);
        });
        expedition.submitForReview();
        new ApproveExpedition(new ApprovalPolicy(List.of()), reservations).approve(expedition, List.of());
    }

    @Test
    void finishingEarlyFreesTheReservationsOfTheActivitiesThatDidNotRun() {
        expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(14, 16));
        approveEquipped();
        expedition.start();

        finishExpedition.finish(expedition);

        assertEquals(ExpeditionStatus.FINISHED, expedition.getStatus());
        assertTrue(worker.isAvailableDuring(hours(14, 16)));
    }

    @Test
    void anExpeditionThatCannotFinishKeepsItsReservations() {
        expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(14, 16));
        approveEquipped();

        assertThrows(InvalidStatusTransitionException.class, () -> finishExpedition.finish(expedition));

        assertEquals(ExpeditionStatus.APPROVED, expedition.getStatus());
        assertFalse(worker.isAvailableDuring(hours(14, 16)));
    }

    @Test
    void anExpeditionWithAnActivityInProgressKeepsItsReservations() {
        final var item = expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(9, 11));
        expedition.schedule(sampling("a-2", coast, hours(0, 24)), hours(14, 16));
        approveEquipped();
        expedition.start();
        expedition.startActivity(item, LocalDateTime.of(2026, 10, 1, 9, 0));

        assertThrows(ActivitiesInProgressException.class, () -> finishExpedition.finish(expedition));

        assertEquals(ExpeditionStatus.IN_EXECUTION, expedition.getStatus());
        assertFalse(worker.isAvailableDuring(hours(14, 16)));
    }

    @Test
    void finishingGivesBackTheStockThatWasReservedAndNotUsed() {
        final var jars = supply("d-1", "Sample container", Quantity.of("10", UNITS));
        final var item = expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(14, 16));
        expedition.assignSupply(item, jars);
        approveEquipped();
        expedition.start();

        finishExpedition.finish(expedition);

        assertTrue(jars.hasStockFor(Quantity.of("10", UNITS)));
    }
}
