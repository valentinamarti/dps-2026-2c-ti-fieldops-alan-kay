package ar.edu.itba.dps.fieldops.business.fixtures;

import ar.edu.itba.dps.fieldops.business.exceptions.DuplicateZoneException;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class ZoneRepositoryContract {
    protected ZoneRepository repository;

    protected abstract ZoneRepository createRepository();

    @BeforeEach
    void setUpRepository() {
        repository = createRepository();
    }

    @Test
    void findsTheVeryZoneThatWasAdded() {
        final Zone reef = zone("z-1");
        repository.add(reef);

        assertSame(reef, repository.findById("z-1").orElseThrow());
    }

    @Test
    void findsNothingForAnUnknownId() {
        assertEquals(Optional.empty(), repository.findById("z-404"));
    }

    @Test
    void rejectsASecondZoneWithTheSameId() {
        repository.add(zone("z-1"));

        assertThrows(DuplicateZoneException.class, () -> repository.add(zone("z-1")));
    }

    @Test
    void listsNothingWhenNothingWasAdded() {
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void listsEveryZoneAddedInTheOrderTheyWereAdded() {
        final var first = zone("z-1");
        final var second = zone("z-2");
        repository.add(first);
        repository.add(second);

        assertEquals(List.of(first, second), repository.findAll());
    }
}
