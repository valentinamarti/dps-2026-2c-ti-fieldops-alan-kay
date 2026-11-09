package ar.edu.itba.dps.fieldops.business.integration;

import ar.edu.itba.dps.fieldops.business.interfaces.events.AsyncEventSubscriber;
import ar.edu.itba.dps.fieldops.business.interfaces.events.DomainEvent;
import ar.edu.itba.dps.fieldops.business.models.events.ConsumerName;
import ar.edu.itba.dps.fieldops.business.models.events.EventId;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

final class RecordingConsumer implements AsyncEventSubscriber<DomainEvent> {
    static final ConsumerName NAME = new ConsumerName("test-recorder");

    private final List<EventId> received = new CopyOnWriteArrayList<>();

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
        recordOnCommit(received, event);
    }

    List<EventId> received() {
        return List.copyOf(received);
    }

    void forget() {
        received.clear();
    }

    static void recordOnCommit(List<EventId> received, DomainEvent event) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                received.add(event.id());
            }
        });
    }
}
