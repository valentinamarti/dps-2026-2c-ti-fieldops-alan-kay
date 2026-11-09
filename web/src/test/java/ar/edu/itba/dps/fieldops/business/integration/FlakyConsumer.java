package ar.edu.itba.dps.fieldops.business.integration;

import ar.edu.itba.dps.fieldops.business.interfaces.events.AsyncEventSubscriber;
import ar.edu.itba.dps.fieldops.business.interfaces.events.DomainEvent;
import ar.edu.itba.dps.fieldops.business.models.events.ConsumerName;
import ar.edu.itba.dps.fieldops.business.models.events.EventId;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

final class FlakyConsumer implements AsyncEventSubscriber<DomainEvent> {
    static final ConsumerName NAME = new ConsumerName("test-flaky");
    static final String FAILURE = "the flaky consumer failed after writing its effect";

    private final List<EventId> received = new CopyOnWriteArrayList<>();
    private final Map<EventId, Queue<RuntimeException>> failures = new ConcurrentHashMap<>();

    @Override
    public Class<DomainEvent> eventType() {
        return DomainEvent.class;
    }

    @Override
    public ConsumerName consumerName() {
        return NAME;
    }

    @Override
    public void on(DomainEvent event) {
        RecordingConsumer.recordOnCommit(received, event);
        final var failure = failuresFor(event.id()).poll();
        if (failure != null) {
            throw failure;
        }
    }

    void failTimes(EventId event, int times) {
        for (var time = 0; time < times; time++) {
            failWith(event, new IllegalStateException(FAILURE));
        }
    }

    void failWith(EventId event, RuntimeException failure) {
        failuresFor(event).add(failure);
    }

    List<EventId> received() {
        return List.copyOf(received);
    }

    void forget() {
        received.clear();
        failures.clear();
    }

    private Queue<RuntimeException> failuresFor(EventId event) {
        return failures.computeIfAbsent(event, _ -> new ConcurrentLinkedQueue<>());
    }
}
