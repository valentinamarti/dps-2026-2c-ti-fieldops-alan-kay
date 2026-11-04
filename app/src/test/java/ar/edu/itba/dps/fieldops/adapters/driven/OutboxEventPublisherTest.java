package ar.edu.itba.dps.fieldops.adapters.driven;

import ar.edu.itba.dps.fieldops.adapters.driven.persistence.DomainEventEntity;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.DomainEventJpaRepository;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.EventDeliveryEntity;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.EventDeliveryJpaRepository;
import ar.edu.itba.dps.fieldops.business.interfaces.events.AsyncEventSubscriber;
import ar.edu.itba.dps.fieldops.business.interfaces.events.EventSubscriber;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.events.ActivityCompleted;
import ar.edu.itba.dps.fieldops.business.models.events.ConsumerName;
import ar.edu.itba.dps.fieldops.business.models.events.DeliveryStatus;
import ar.edu.itba.dps.fieldops.business.models.events.EventId;
import ar.edu.itba.dps.fieldops.business.models.events.ExpeditionApproved;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxEventPublisherTest {
    private static final ExpeditionId EXPEDITION = new ExpeditionId("e-1");
    private static final LocalDateTime TEN = LocalDateTime.of(2026, 10, 1, 10, 0);
    private static final LocalDateTime RECORDED_AT = LocalDateTime.of(2026, 10, 1, 11, 30);
    private static final Clock CLOCK = Clock.fixed(RECORDED_AT.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    private static final ExpeditionApproved APPROVAL = new ExpeditionApproved(new EventId("ev-1"), EXPEDITION, TEN);
    private static final ActivityCompleted COMPLETION =
            new ActivityCompleted(new EventId("ev-2"), EXPEDITION, new ActivityId("a-08"), TEN, "Twelve samples");

    private static final class Recorder implements EventSubscriber<ExpeditionApproved> {
        private final String name;
        private final List<String> log;

        Recorder(String name, List<String> log) {
            this.name = name;
            this.log = log;
        }

        @Override
        public Class<ExpeditionApproved> eventType() {
            return ExpeditionApproved.class;
        }

        @Override
        public void on(ExpeditionApproved event) {
            log.add(name + ":" + event.id());
        }
    }

    private static final class AsyncRecorder implements AsyncEventSubscriber<ExpeditionApproved> {
        private final ConsumerName name;
        private final List<String> log;

        AsyncRecorder(String name, List<String> log) {
            this.name = new ConsumerName(name);
            this.log = log;
        }

        @Override
        public Class<ExpeditionApproved> eventType() {
            return ExpeditionApproved.class;
        }

        @Override
        public ConsumerName consumerName() {
            return name;
        }

        @Override
        public void on(ExpeditionApproved event) {
            log.add(name + ":" + event.id());
        }
    }

    private final DomainEventJpaRepository events = mock(DomainEventJpaRepository.class);
    private final EventDeliveryJpaRepository deliveries = mock(EventDeliveryJpaRepository.class);
    private final EventSerializer serializer = new EventSerializer();
    private long sequences;

    @BeforeEach
    void assignASequenceWhenAnEventIsSaved() {
        when(events.save(any(DomainEventEntity.class))).thenAnswer(call -> {
            final DomainEventEntity saved = call.getArgument(0);
            saved.setSequence(++sequences);
            return saved;
        });
    }

    @Test
    void deliversTheEventToEverySubscriberOfItsTypeInOrder() {
        final var log = new ArrayList<String>();
        final var publisher = publisherWith(List.of(new Recorder("first", log), new Recorder("second", log)), List.of());

        publisher.publish(APPROVAL);

        assertEquals(List.of("first:ev-1", "second:ev-1"), log);
    }

    @Test
    void ignoresSubscribersOfOtherEventTypesAndStillRecordsTheEvent() {
        final var log = new ArrayList<String>();
        final var publisher = publisherWith(List.of(new Recorder("first", log)), List.of());

        publisher.publish(COMPLETION);

        assertEquals(List.of(), log);
        assertEquals(ActivityCompleted.class.getName(), recordedEvent().getEventType());
    }

    @Test
    void aFailingSubscriberReachesWhoeverPublishedAndLeavesNothingRecorded() {
        final EventSubscriber<ExpeditionApproved> failing = new EventSubscriber<>() {
            @Override
            public Class<ExpeditionApproved> eventType() {
                return ExpeditionApproved.class;
            }

            @Override
            public void on(ExpeditionApproved event) {
                throw new IllegalStateException("boom");
            }
        };
        final var publisher = publisherWith(List.of(failing), List.of(new AsyncRecorder("notify", new ArrayList<>())));

        assertThrows(IllegalStateException.class, () -> publisher.publish(APPROVAL));

        verify(events, never()).save(any(DomainEventEntity.class));
        verify(deliveries, never()).save(any(EventDeliveryEntity.class));
    }

    @Test
    void recordsTheEventWithEnoughToReadItBack() {
        publisherWith(List.of(), List.of()).publish(APPROVAL);

        final var recorded = recordedEvent();

        assertEquals("ev-1", recorded.getEventId());
        assertEquals("e-1", recorded.getExpeditionId());
        assertEquals(ExpeditionApproved.class.getName(), recorded.getEventType());
        assertEquals(TEN, recorded.getOccurredAt());
        assertEquals(RECORDED_AT, recorded.getRecordedAt());
        assertEquals(APPROVAL, serializer.deserialize(recorded.getEventType(), recorded.getPayload()));
    }

    @Test
    void doesNotDeliverToAnAsyncSubscriberButLeavesItAPendingDelivery() {
        final var log = new ArrayList<String>();

        publisherWith(List.of(), List.of(new AsyncRecorder("notify-responsibles", log))).publish(APPROVAL);

        assertTrue(log.isEmpty());
        final var pending = pendingDeliveries().getFirst();
        assertEquals("notify-responsibles", pending.getId().getConsumer());
        assertEquals(1L, pending.getId().getEventSequence());
        assertEquals(DeliveryStatus.PENDING.name(), pending.getStatus());
        assertEquals(0, pending.getAttempts());
        assertEquals(RECORDED_AT, pending.getNextAttemptAt());
        assertNull(pending.getDeliveredAt());
        assertNull(pending.getLastError());
    }

    @Test
    void leavesOnePendingDeliveryPerInterestedConsumer() {
        final var log = new ArrayList<String>();
        final var publisher = publisherWith(List.of(),
                List.of(new AsyncRecorder("notify-responsibles", log), new AsyncRecorder("track-progress", log)));

        publisher.publish(APPROVAL);

        assertEquals(List.of("notify-responsibles", "track-progress"),
                pendingDeliveries().stream().map(delivery -> delivery.getId().getConsumer()).toList());
    }

    @Test
    void leavesNothingForAConsumerThatIsNotInterestedInThatEvent() {
        final var publisher = publisherWith(List.of(), List.of(new AsyncRecorder("notify-responsibles", new ArrayList<>())));

        publisher.publish(COMPLETION);

        assertEquals(ActivityCompleted.class.getName(), recordedEvent().getEventType());
        verify(deliveries, never()).save(any(EventDeliveryEntity.class));
    }

    @Test
    void recordsTheEventEvenWithoutAnyAsyncConsumer() {
        publisherWith(List.of(), List.of()).publish(APPROVAL);

        assertEquals("ev-1", recordedEvent().getEventId());
        verify(deliveries, never()).save(any(EventDeliveryEntity.class));
    }

    private OutboxEventPublisher publisherWith(List<EventSubscriber<?>> synchronous,
                                               List<AsyncEventSubscriber<?>> asynchronous) {
        return new OutboxEventPublisher(synchronous, asynchronous, events, deliveries, serializer, CLOCK);
    }

    private DomainEventEntity recordedEvent() {
        final var captor = ArgumentCaptor.forClass(DomainEventEntity.class);
        verify(events).save(captor.capture());
        return captor.getValue();
    }

    private List<EventDeliveryEntity> pendingDeliveries() {
        final var captor = ArgumentCaptor.forClass(EventDeliveryEntity.class);
        verify(deliveries, atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }
}
