package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.NotFoundException;
import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionRestrictions;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateExpeditionCommandTest {

    private final ExpeditionRepository repository = spy(new SequentialExpeditionRepository());
    private final ZoneRepository zones = mock(ZoneRepository.class);
    private final ResourceCatalog catalog = new ResourceCatalog();
    private final CreateExpedition createExpedition = new CreateExpedition(repository, zones, catalog);
    private final Zone coast = zone("z-1");

    private CreateExpeditionCommand commandFor(String zoneId, String responsibleId) {
        return new CreateExpeditionCommand("Coastal survey", hours(0, 24), List.of("Map the reef"), Set.of(zoneId),
                Set.of(new ResourceId(responsibleId)), Set.of(new Permit("Marine reserve access")),
                new ExpeditionRestrictions(10, RiskLevel.MEDIUM));
    }

    @Test
    void draftsAndStoresAnExpeditionFromTheIdsInTheCommand() {
        final var leader = person("p-1");
        catalog.addStaff(leader);
        when(zones.findById("z-1")).thenReturn(Optional.of(coast));

        final var expedition = createExpedition.create(commandFor("z-1", "p-1"));

        assertEquals(ExpeditionStatus.DRAFT, expedition.getStatus());
        assertEquals(Set.of(coast), expedition.getZones());
        assertEquals(Set.of(leader), expedition.getResponsibles());
        assertEquals(Set.of(new Permit("Marine reserve access")), expedition.getGrantedPermits());
        assertSame(expedition, repository.findById(expedition.getId()).orElseThrow());
    }

    @Test
    void failsWithoutStoringAnythingWhenTheZoneDoesNotExist() {
        catalog.addStaff(person("p-1"));
        when(zones.findById("z-404")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> createExpedition.create(commandFor("z-404", "p-1")));
        verify(repository, never()).add(any());
    }

    @Test
    void failsWithoutStoringAnythingWhenTheResponsibleIsNotInTheCatalog() {
        when(zones.findById("z-1")).thenReturn(Optional.of(coast));

        assertThrows(NotFoundException.class, () -> createExpedition.create(commandFor("z-1", "p-404")));
        verify(repository, never()).add(any());
    }
}
