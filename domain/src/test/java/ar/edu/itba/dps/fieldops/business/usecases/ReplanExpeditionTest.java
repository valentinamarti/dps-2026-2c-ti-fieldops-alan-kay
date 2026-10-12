package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.replanning.Cancellation;
import ar.edu.itba.dps.fieldops.business.replanning.ExpeditionReplanner;
import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.completeAssignments;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ReplanExpeditionTest {


    private final SequentialExpeditionRepository repository = new SequentialExpeditionRepository();
    private final ReplanExpedition replanExpedition = new ReplanExpedition(repository, new ExpeditionReplanner(repository));
    private final Zone coast = zone("z-1");

    @Test
    void theAlternativeGetsANewIdFromTheSystemAndIsStored() {
        final var original = new CreateExpedition(repository).create(expeditionIn(coast, person("p-1")));
        original.schedule(sampling("a-1", coast, hours(8, 18)), hours(9, 11));
        final var affectedItem = original.schedule(sampling("a-2", coast, hours(8, 18)), hours(12, 14));
        completeAssignments(original);
        original.submitForReview();
        original.approve(List.of(), List.of());

        final var alternative = replanExpedition.replan(original, affectedItem, new Cancellation());

        assertNotEquals(original.getId(), alternative.getId());
        assertEquals(ExpeditionStatus.DRAFT, alternative.getStatus());
        assertSame(alternative, repository.findById(alternative.getId()).orElseThrow());
    }
}
