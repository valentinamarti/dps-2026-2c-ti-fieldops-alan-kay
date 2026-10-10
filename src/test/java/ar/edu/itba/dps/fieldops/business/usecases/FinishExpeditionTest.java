package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.ActivitiesInProgressException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidStatusTransitionException;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.validation.ApprovalResult;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.services.ResourceReservations;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
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

class FinishExpeditionTest {

    private final ResourceReservations reservations = new ResourceReservations();
    private final FinishExpedition finishExpedition = new FinishExpedition(reservations);
    private final Zone coast = zone("z-1");
    private final Expedition expedition = expeditionIn(coast, person("p-1")).build();
    private final Person worker = person("p-2");

    private void approveWithWorkerFor(ItineraryItem item) {
        expedition.assignStaff(item, worker);
        expedition.submitForReview();
        new ApproveExpedition(anyExpedition -> new ApprovalResult(List.of()), reservations).approve(expedition, List.of());
    }

    @Test
    void finishingEarlyFreesTheReservationsOfTheActivitiesThatDidNotRun() {
        final var laterItem = expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(14, 16));
        approveWithWorkerFor(laterItem);
        expedition.start();

        finishExpedition.finish(expedition);

        assertEquals(ExpeditionStatus.FINISHED, expedition.getStatus());
        assertTrue(worker.isAvailableDuring(hours(14, 16)));
    }

    @Test
    void anExpeditionThatCannotFinishKeepsItsReservations() {
        final var item = expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(14, 16));
        approveWithWorkerFor(item);

        assertThrows(InvalidStatusTransitionException.class, () -> finishExpedition.finish(expedition));

        assertEquals(ExpeditionStatus.APPROVED, expedition.getStatus());
        assertFalse(worker.isAvailableDuring(hours(14, 16)));
    }

    @Test
    void anExpeditionWithAnActivityInProgressKeepsItsReservations() {
        final var item = expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(9, 11));
        final var laterItem = expedition.schedule(sampling("a-2", coast, hours(0, 24)), hours(14, 16));
        expedition.assignStaff(laterItem, worker);
        approveWithWorkerFor(item);
        expedition.start();
        expedition.startActivity(item, LocalDateTime.of(2026, 10, 1, 9, 0));

        assertThrows(ActivitiesInProgressException.class, () -> finishExpedition.finish(expedition));

        assertEquals(ExpeditionStatus.IN_EXECUTION, expedition.getStatus());
        assertFalse(worker.isAvailableDuring(hours(14, 16)));
    }
}
