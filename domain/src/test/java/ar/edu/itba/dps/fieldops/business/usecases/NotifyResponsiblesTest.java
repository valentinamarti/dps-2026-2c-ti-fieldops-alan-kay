package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.NotFoundException;
import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.interfaces.events.DomainEvent;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.events.ActivityCompleted;
import ar.edu.itba.dps.fieldops.business.models.events.ConsumerName;
import ar.edu.itba.dps.fieldops.business.models.events.EventId;
import ar.edu.itba.dps.fieldops.business.models.events.ExpeditionApproved;
import ar.edu.itba.dps.fieldops.business.models.events.IncidentRegistered;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentId;
import ar.edu.itba.dps.fieldops.business.models.notifications.Notification;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.providers.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotifyResponsiblesTest {
    private static final EventId EVENT = new EventId("ev-1");
    private static final ActivityId ACTIVITY = new ActivityId("a-08");
    private static final LocalDateTime TEN = LocalDateTime.of(2026, 10, 1, 10, 0);

    private static final class RecordingNotifications implements NotificationRepository {
        private final List<Notification> added = new ArrayList<>();

        @Override
        public void add(Notification notification) {
            added.add(notification);
        }

        @Override
        public List<Notification> ofExpedition(ExpeditionId expedition) {
            return added.stream().filter(notification -> notification.expedition().equals(expedition)).toList();
        }
    }

    private final SequentialExpeditionRepository expeditions = new SequentialExpeditionRepository();
    private final RecordingNotifications notifications = new RecordingNotifications();
    private final NotifyResponsibles consumer = new NotifyResponsibles(expeditions, notifications);

    static Stream<Function<ExpeditionId, DomainEvent>> everyEvent() {
        return Stream.of(
                expedition -> new ExpeditionApproved(EVENT, expedition, TEN),
                expedition -> new ActivityCompleted(EVENT, expedition, ACTIVITY, TEN, "Twelve samples collected"),
                expedition -> new IncidentRegistered(EVENT, expedition, new Incident(new IncidentId(expedition, 1), ACTIVITY,
                        "A storm flooded the station", TEN, Optional.empty(), Optional.empty())));
    }

    private ExpeditionId expeditionWithResponsibles(String first, String second) {
        final var expedition = expeditionIn(zone("z-1"), person(first)).addResponsible(person(second)).build(expeditions);
        expeditions.add(expedition);
        return expedition.getId();
    }

    @ParameterizedTest
    @MethodSource("everyEvent")
    void everyResponsibleGetsOneNotificationWithWhatTheEventSays(Function<ExpeditionId, DomainEvent> eventOn) {
        final var expedition = expeditionWithResponsibles("p-1", "p-2");
        final var event = eventOn.apply(expedition);

        consumer.on(event);

        assertEquals(2, notifications.added.size());
        assertEquals(Set.of(new ResourceId("p-1"), new ResourceId("p-2")),
                notifications.added.stream().map(Notification::responsible).collect(Collectors.toSet()));
        assertTrue(notifications.added.stream().allMatch(notification -> notification.equals(
                new Notification(EVENT, expedition, notification.responsible(), event.summary(), TEN))));
    }

    @Test
    void anExpeditionThatDoesNotExistFailsAndNotifiesNobody() {
        final var event = new ExpeditionApproved(EVENT, new ExpeditionId("e-9"), TEN);

        assertThrows(NotFoundException.class, () -> consumer.on(event));

        assertEquals(List.of(), notifications.added);
    }

    @Test
    void asksForEveryEventUnderAStableName() {
        assertEquals(DomainEvent.class, consumer.eventType());
        assertEquals(new ConsumerName("notify-responsibles"), consumer.consumerName());
    }
}
