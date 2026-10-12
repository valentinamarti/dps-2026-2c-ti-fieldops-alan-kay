package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.activity;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.parallel;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.sequential;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ItineraryCascadeTest {

    private final Zone coast = zone("z-1");
    private final Expedition expedition = expeditionIn(coast, person("p-1")).build(new SequentialExpeditionRepository());
    private final Itinerary itinerary = expedition.getItinerary();

    private Activity activityAt(String id, Activity... dependencies) {
        return sampling(id, coast, hours(0, 24), dependencies);
    }

    private ItineraryItem itemOf(Activity activity) {
        return itinerary.itemFor(activity).orElseThrow();
    }

    @Test
    void theDependentsOfAnActivityIncludeTheIndirectOnes() {
        final var root = activityAt("a-1");
        final var direct = activityAt("a-2", root);
        final var indirect = activityAt("a-3", direct);
        final var unrelated = activityAt("a-4");
        expedition.schedule(root, hours(8, 9));
        expedition.schedule(direct, hours(9, 10));
        expedition.schedule(indirect, hours(10, 11));
        expedition.schedule(unrelated, hours(10, 11));

        assertEquals(List.of(itemOf(direct), itemOf(indirect)), itinerary.dependentsOf(itemOf(root)));
        assertEquals(List.of(), itinerary.dependentsOf(itemOf(unrelated)));
    }

    @Test
    void whatComesAfterAnActivityInASequentialBlockMovesWithIt() {
        final var first = activityAt("a-1");
        final var second = activityAt("a-2");
        final var third = activityAt("a-3");
        expedition.schedule(sequential(activity(first, hours(8, 9)), activity(second, hours(9, 10)), activity(third, hours(10, 11))));

        assertEquals(List.of(itemOf(third)), itinerary.movingWith(itemOf(second)));
        assertEquals(List.of(), itinerary.movingWith(itemOf(third)));
    }

    @Test
    void aParallelBranchDoesNotMoveWithItsSibling() {
        final var left = activityAt("a-1");
        final var right = activityAt("a-2");
        final var after = activityAt("a-3");
        expedition.schedule(sequential(parallel(activity(left, hours(8, 9)), activity(right, hours(8, 9))), activity(after, hours(9, 10))));

        assertEquals(List.of(itemOf(after)), itinerary.movingWith(itemOf(left)));
    }

    @Test
    void whatMovesIncludesTheDependentsAndWhatComesAfterThem() {
        final var root = activityAt("a-1");
        final var dependent = activityAt("a-2", root);
        final var afterDependent = activityAt("a-3");
        final var loose = activityAt("a-4");
        expedition.schedule(root, hours(8, 9));
        expedition.schedule(sequential(activity(dependent, hours(9, 10)), activity(afterDependent, hours(10, 11))));
        expedition.schedule(loose, hours(9, 10));

        assertEquals(List.of(itemOf(dependent), itemOf(afterDependent)), itinerary.movingWith(itemOf(root)));
    }

    @Test
    void aLooseActivityMovesAlone() {
        final var loose = activityAt("a-1");
        expedition.schedule(loose, hours(8, 9));
        expedition.schedule(activityAt("a-2"), hours(9, 10));

        assertEquals(List.of(), itinerary.movingWith(itemOf(loose)));
    }
}
