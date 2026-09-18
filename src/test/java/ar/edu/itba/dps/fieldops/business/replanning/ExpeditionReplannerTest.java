package ar.edu.itba.dps.fieldops.business.replanning;

import ar.edu.itba.dps.fieldops.business.exceptions.InvalidScheduleException;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpeditionReplannerTest {

    private final ExpeditionReplanner replanner = new ExpeditionReplanner();
    private final Zone coast = zone("z-1");
    private final Person leader = person("p-1");

    @Test
    void keepsUnaffectedItemsWithTheirAssignmentsAndDropsTheAffectedOne() {
        final var expedition = expeditionIn(coast, leader).build();
        final var keptActivity = sampling("a-1", coast, hours(8, 18));
        final var affectedActivity = sampling("a-2", coast, hours(8, 18));
        final var keptItem = expedition.schedule(keptActivity, hours(9, 11));
        final var affectedItem = expedition.schedule(affectedActivity, hours(12, 14));
        final var worker = person("p-2");
        expedition.assignStaff(keptItem, worker);

        final var alternative = replanner.planAlternative(expedition, "e-1-alt", affectedItem);

        assertEquals(ExpeditionStatus.DRAFT, alternative.getStatus());
        assertEquals(1, alternative.getItinerary().getItems().size());
        final var copiedItem = alternative.getItinerary().itemFor(keptActivity).orElseThrow();
        assertEquals(Set.of(worker), copiedItem.getAssignedStaff());
        assertTrue(alternative.getItinerary().itemFor(affectedActivity).isEmpty());
    }

    @Test
    void rejectsAnItemThatDoesNotBelongToTheOriginalExpedition() {
        final var expedition = expeditionIn(coast, leader).build();
        final var otherExpedition = expeditionIn(coast, leader).build();
        final var foreignItem = otherExpedition.schedule(sampling("a-1", coast, hours(8, 18)), hours(9, 11));

        assertThrows(IllegalArgumentException.class,
                () -> replanner.planAlternative(expedition, "e-1-alt", foreignItem));
    }

    @Test
    void failsWhenAScheduledActivityDependsOnTheAffectedOne() {
        final var expedition = expeditionIn(coast, leader).build();
        final var dependency = sampling("a-1", coast, hours(8, 18));
        final var dependent = sampling("a-2", coast, hours(8, 18), dependency);
        final var affectedItem = expedition.schedule(dependency, hours(9, 11));
        expedition.schedule(dependent, hours(12, 14));

        assertThrows(InvalidScheduleException.class,
                () -> replanner.planAlternative(expedition, "e-1-alt", affectedItem));
    }
}
