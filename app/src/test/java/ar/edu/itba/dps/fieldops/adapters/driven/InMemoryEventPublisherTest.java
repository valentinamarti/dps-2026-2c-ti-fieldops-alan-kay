package ar.edu.itba.dps.fieldops.adapters.driven;

import ar.edu.itba.dps.fieldops.business.interfaces.events.DomainEvent;
import ar.edu.itba.dps.fieldops.business.interfaces.events.EventSubscriber;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryEventPublisherTest {

    private record Happened(String what) implements DomainEvent {
    }

    private record SomethingElse() implements DomainEvent {
    }

    private static final class Recorder implements EventSubscriber<Happened> {

        private final String name;
        private final List<String> log;

        Recorder(String name, List<String> log) {
            this.name = name;
            this.log = log;
        }

        @Override
        public Class<Happened> eventType() {
            return Happened.class;
        }

        @Override
        public void on(Happened event) {
            log.add(name + ":" + event.what());
        }
    }

    @Test
    void deliversTheEventToEverySubscriberOfItsTypeInOrderBeforeReturning() {
        final var log = new ArrayList<String>();
        final var publisher = new InMemoryEventPublisher(List.of(new Recorder("first", log), new Recorder("second", log)));

        publisher.publish(new Happened("incident"));

        assertEquals(List.of("first:incident", "second:incident"), log);
    }

    @Test
    void ignoresSubscribersOfOtherEventTypes() {
        final var log = new ArrayList<String>();
        final var publisher = new InMemoryEventPublisher(List.of(new Recorder("first", log)));

        publisher.publish(new SomethingElse());

        assertEquals(List.of(), log);
    }

    @Test
    void aFailingSubscriberReachesWhoeverPublished() {
        final EventSubscriber<Happened> failing = new EventSubscriber<>() {
            @Override
            public Class<Happened> eventType() {
                return Happened.class;
            }

            @Override
            public void on(Happened event) {
                throw new IllegalStateException("boom");
            }
        };
        final var publisher = new InMemoryEventPublisher(List.of(failing));

        assertThrows(IllegalStateException.class, () -> publisher.publish(new Happened("incident")));
    }
}
