package ar.edu.itba.dps.fieldops.providers;

import ar.edu.itba.dps.fieldops.business.exceptions.DuplicateExpeditionException;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.stream.IntStream;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryExpeditionRepositoryTest {

    private final InMemoryExpeditionRepository repository = new InMemoryExpeditionRepository();
    private final Zone coast = zone("z-1");
    private final Person leader = person("p-1");

    @Test
    void neverHandsOutTheSameIdTwice() {
        final var ids = new HashSet<ExpeditionId>();

        IntStream.range(0, 100).forEach(_ -> ids.add(repository.nextId()));

        assertEquals(100, ids.size());
    }

    @Test
    void rejectsASecondExpeditionWithAnIdAlreadyTaken() {
        final var id = repository.nextId();
        repository.add(expeditionIn(coast, leader).build(id));
        final var impostor = expeditionIn(coast, leader).build(id);

        assertThrows(DuplicateExpeditionException.class, () -> repository.add(impostor));
    }

    @Test
    void findsTheVeryExpeditionThatWasAdded() {
        final var expedition = expeditionIn(coast, leader).build(repository.nextId());
        repository.add(expedition);

        assertSame(expedition, repository.findById(expedition.getId()).orElseThrow());
    }

    @Test
    void findsNothingForAnUnknownId() {
        assertEquals(Optional.empty(), repository.findById(repository.nextId()));
    }
}
