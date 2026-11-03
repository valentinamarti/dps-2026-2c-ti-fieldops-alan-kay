package ar.edu.itba.dps.fieldops.business.services;

import ar.edu.itba.dps.fieldops.business.interfaces.events.DomainEvent;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.providers.EventPublisher;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublishingExpeditionRepositoryTest {

    private final ExpeditionRepository stored = mock(ExpeditionRepository.class);
    private final EventPublisher publisher = mock(EventPublisher.class);
    private final ExpeditionRepository expeditions = new PublishingExpeditionRepository(stored, publisher);
    private final Expedition expedition = mock(Expedition.class);
    private final DomainEvent first = mock(DomainEvent.class);
    private final DomainEvent second = mock(DomainEvent.class);

    @Test
    void savingPublishesEveryEventTheExpeditionRecordedInOrder() {
        when(expedition.pullEvents()).thenReturn(List.of(first, second));

        expeditions.save(expedition);

        final var order = inOrder(publisher);
        order.verify(publisher).publish(first);
        order.verify(publisher).publish(second);
    }

    @Test
    void addingPublishesTheEventsTheExpeditionRecordedToo() {
        when(expedition.pullEvents()).thenReturn(List.of(first));

        expeditions.add(expedition);

        verify(stored).add(expedition);
        verify(publisher).publish(first);
    }

    @Test
    void storesTheExpeditionBeforePublishingItsEvents() {
        when(expedition.pullEvents()).thenReturn(List.of(first));

        expeditions.save(expedition);

        final var order = inOrder(stored, publisher);
        order.verify(stored).save(expedition);
        order.verify(publisher).publish(first);
    }

    @Test
    void anExpeditionThatRecordedNothingPublishesNothing() {
        when(expedition.pullEvents()).thenReturn(List.of());

        expeditions.save(expedition);

        verify(publisher, never()).publish(any());
    }

    @Test
    void ifTheExpeditionCannotBeStoredItsEventsAreNeitherTakenNorPublished() {
        doThrow(new IllegalStateException("the database is down")).when(stored).save(expedition);

        assertThrows(IllegalStateException.class, () -> expeditions.save(expedition));
        verify(expedition, never()).pullEvents();
        verify(publisher, never()).publish(any());
    }

    @Test
    void readingGoesStraightToTheStoredExpeditions() {
        final var id = new ExpeditionId("e-1");
        when(stored.nextId()).thenReturn(id);
        when(stored.findById(id)).thenReturn(Optional.of(expedition));
        when(stored.findAll()).thenReturn(List.of(expedition));

        assertEquals(id, expeditions.nextId());
        assertEquals(Optional.of(expedition), expeditions.findById(id));
        assertEquals(List.of(expedition), expeditions.findAll());
        verify(publisher, never()).publish(any());
    }
}
