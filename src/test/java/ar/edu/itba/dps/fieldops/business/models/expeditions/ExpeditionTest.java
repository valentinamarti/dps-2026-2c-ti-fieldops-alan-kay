package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.exceptions.ExpeditionNotEditableException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidScheduleException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidStatusTransitionException;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.resources.Depletable;
import ar.edu.itba.dps.fieldops.business.models.resources.Instrument;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExpeditionTest {

    private final Zone coast = zone("z-1");
    private final Person leader = person("p-1");
    private final Expedition expedition = expeditionIn(coast, leader).build();

    @Test
    void schedulesAnActivityOfOneOfItsZonesWithinItsPeriod() {
        final var item = expedition.schedule(sampling("a-1", coast, hours(8, 18)), hours(9, 11));

        assertEquals(List.of(item), expedition.getItinerary().getItems());
    }

    @Test
    void rejectsAnActivityInAZoneThatIsNotPartOfTheExpedition() {
        final var elsewhere = sampling("a-1", zone("z-2"), hours(8, 18));

        assertThrows(InvalidScheduleException.class, () -> expedition.schedule(elsewhere, hours(9, 11)));
    }

    @Test
    void rejectsAnActivityScheduledOutsideTheExpeditionPeriod() {
        final var nightShift = sampling("a-1", coast, hours(20, 30));

        assertThrows(InvalidScheduleException.class, () -> expedition.schedule(nightShift, hours(23, 25)));
    }

    @Test
    void assignsResourcesToAnItemOfItsItinerary() {
        final var item = expedition.schedule(sampling("a-1", coast, hours(8, 18)), hours(9, 11));
        final var worker = person("p-2");
        final var cooler = new Instrument("i-1", "Cooler 40L", new ResourceCategory("Cooler"), Set.of());
        final var containers = new Depletable("d-1", "Jars", new ResourceCategory("Sample container"), Quantity.of("10", UNITS));

        expedition.assignStaff(item, worker);
        expedition.assignEquipment(item, cooler);
        expedition.assignSupply(item, containers);

        assertEquals(Set.of(worker), item.getAssignedStaff());
        assertEquals(Set.of(cooler), item.getAssignedEquipment());
        assertEquals(Set.of(containers), item.getAssignedSupplies());
    }

    @Test
    void assigningTheSameResourceTwiceKeepsASingleAssignment() {
        final var item = expedition.schedule(sampling("a-1", coast, hours(8, 18)), hours(9, 11));
        final var worker = person("p-2");

        expedition.assignStaff(item, worker);
        expedition.assignStaff(item, worker);

        assertEquals(Set.of(worker), item.getAssignedStaff());
    }

    @Test
    void rejectsAssignmentsToAnItemOfAnotherExpedition() {
        final var otherExpedition = expeditionIn(coast, leader).build();
        final var foreignItem = otherExpedition.schedule(sampling("a-1", coast, hours(8, 18)), hours(9, 11));

        assertThrows(IllegalArgumentException.class, () -> expedition.assignStaff(foreignItem, person("p-2")));
    }

    @Test
    void canBeSentToReviewAndBackToDraft() {
        expedition.submitForReview();
        assertEquals(ExpeditionStatus.IN_REVIEW, expedition.getStatus());

        expedition.returnToDraft();
        assertEquals(ExpeditionStatus.DRAFT, expedition.getStatus());
    }

    @Test
    void itsItineraryCannotChangeWhileInReview() {
        final var item = expedition.schedule(sampling("a-1", coast, hours(8, 18)), hours(9, 11));
        expedition.submitForReview();

        assertThrows(ExpeditionNotEditableException.class,
                () -> expedition.schedule(sampling("a-2", coast, hours(8, 18)), hours(12, 14)));
        assertThrows(ExpeditionNotEditableException.class, () -> expedition.assignStaff(item, person("p-2")));
    }

    @Test
    void cannotBeSubmittedForReviewTwice() {
        expedition.submitForReview();

        assertThrows(InvalidStatusTransitionException.class, expedition::submitForReview);
    }
}
