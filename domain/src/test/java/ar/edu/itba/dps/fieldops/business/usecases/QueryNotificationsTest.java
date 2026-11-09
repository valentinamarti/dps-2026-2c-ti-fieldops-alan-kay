package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.NotFoundException;
import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.models.events.EventId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.notifications.Notification;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.providers.NotificationRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class QueryNotificationsTest {
    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final SequentialExpeditionRepository expeditions = new SequentialExpeditionRepository();
    private final QueryNotifications query = new QueryNotifications(notifications, expeditions);

    @Test
    void handsOutTheNotificationsOfTheExpedition() {
        final var expedition = expeditionIn(zone("z-1"), person("p-1")).build(expeditions);
        expeditions.add(expedition);
        final var approved = new Notification(new EventId("ev-1"), expedition.getId(), new ResourceId("p-1"),
                "The expedition was approved", LocalDateTime.of(2026, 10, 1, 10, 0));
        when(notifications.ofExpedition(expedition.getId())).thenReturn(List.of(approved));

        assertEquals(List.of(approved), query.ofExpedition(expedition.getId()));
    }

    @Test
    void anExpeditionThatDoesNotExistIsNotFoundAndNothingIsRead() {
        assertThrows(NotFoundException.class, () -> query.ofExpedition(new ExpeditionId("e-9")));

        verifyNoInteractions(notifications);
    }
}
