package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import org.junit.jupiter.api.Test;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CreateExpeditionTest {

    private final SequentialExpeditionRepository repository = new SequentialExpeditionRepository();
    private final CreateExpedition createExpedition = new CreateExpedition(repository);
    private final Zone coast = zone("z-1");
    private final Person leader = person("p-1");

    @Test
    void createsADraftThatCanBeFoundByTheIdTheSystemGaveIt() {
        final var expedition = createExpedition.create(expeditionIn(coast, leader));

        assertEquals(ExpeditionStatus.DRAFT, expedition.getStatus());
        assertSame(expedition, repository.findById(expedition.getId()).orElseThrow());
    }

    @Test
    void twoExpeditionsCreatedFromTheSameDataGetDifferentIds() {
        final var first = createExpedition.create(expeditionIn(coast, leader));
        final var second = createExpedition.create(expeditionIn(coast, leader));

        assertNotEquals(first.getId(), second.getId());
    }
}
