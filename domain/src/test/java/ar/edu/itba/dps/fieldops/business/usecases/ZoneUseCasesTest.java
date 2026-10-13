package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.DuplicateZoneException;
import ar.edu.itba.dps.fieldops.business.exceptions.NotFoundException;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ZoneUseCasesTest {
    private static final Permit PROTECTED_AREA = new Permit("Protected area");

    private final ZoneRepository zones = mock(ZoneRepository.class);
    private final RegisterZone registerZone = new RegisterZone(zones);
    private final QueryZones queryZones = new QueryZones(zones);

    @Test
    void registersAZoneWithThePermitsItRequires() {
        final var reef = registerZone.register(new RegisterZoneCommand("z-1", "Coastal reef", Set.of(PROTECTED_AREA)));

        verify(zones).add(reef);
        assertEquals("z-1", reef.getId());
        assertEquals("Coastal reef", reef.getName());
        assertEquals(Set.of(PROTECTED_AREA), reef.getRequiredPermits());
    }

    @Test
    void letsTheRepositoryRejectASecondZoneWithTheSameId() {
        doThrow(new DuplicateZoneException("z-1")).when(zones).add(any(Zone.class));

        assertThrows(DuplicateZoneException.class,
                () -> registerZone.register(new RegisterZoneCommand("z-1", "Coastal reef", Set.of())));
    }

    @Test
    void leavesAnIncompleteCommandToTheZoneItBuilds() {
        final var withoutName = new RegisterZoneCommand("z-1", null, Set.of());

        assertEquals("name is required",
                assertThrows(IllegalArgumentException.class, () -> registerZone.register(withoutName)).getMessage());
    }

    @Test
    void listsTheZonesOfTheRepository() {
        final var reef = zone("z-1");
        when(zones.findAll()).thenReturn(List.of(reef));

        assertEquals(List.of(reef), queryZones.list());
    }

    @Test
    void getsAZoneByItsId() {
        final var reef = zone("z-1");
        when(zones.findById("z-1")).thenReturn(Optional.of(reef));

        assertSame(reef, queryZones.get("z-1"));
    }

    @Test
    void failsWhenThereIsNoZoneWithThatId() {
        when(zones.findById("z-404")).thenReturn(Optional.empty());

        assertEquals("There is no zone with id z-404",
                assertThrows(NotFoundException.class, () -> queryZones.get("z-404")).getMessage());
    }
}
