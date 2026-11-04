package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import ar.edu.itba.dps.fieldops.business.models.events.ActivityCompleted;
import ar.edu.itba.dps.fieldops.business.models.events.ConsumerName;
import ar.edu.itba.dps.fieldops.business.models.events.DeliveryStatus;
import ar.edu.itba.dps.fieldops.business.models.events.EventId;
import ar.edu.itba.dps.fieldops.business.models.events.ExpeditionApproved;
import ar.edu.itba.dps.fieldops.business.models.events.IncidentRegistered;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.responses.EventDeliveryView;
import ar.edu.itba.dps.fieldops.business.models.responses.RecordedEventView;
import ar.edu.itba.dps.fieldops.business.providers.EventLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class JpaEventLogTest {
    private static final ExpeditionId PATAGONIA = new ExpeditionId("e-1");
    private static final ExpeditionId NIGHT_SURVEY = new ExpeditionId("e-2");
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 2, 1, 8, 30);
    private static final LocalDateTime RECORDED_AT = LocalDateTime.of(2026, 2, 1, 9, 0);
    private static final ConsumerName NOTIFICATIONS = new ConsumerName("notify-responsibles");
    private static final ConsumerName PROGRESS = new ConsumerName("track-progress");
    private static final String PAYLOAD = """
            {"id":{"value":"ev-1"},"expeditionId":{"value":"e-1"},"occurredAt":"2026-02-01T08:30:00"}""";

    private final JsonMapper json = JsonMapper.builder().build();

    @Autowired
    private EventLog log;
    @Autowired
    private DomainEventJpaRepository events;
    @Autowired
    private EventDeliveryJpaRepository deliveries;
    @Autowired
    private DatabaseCleaner cleaner;

    @BeforeEach
    void emptyTheLog() {
        cleaner.clean();
    }

    @Test
    void findsNothingForAnExpeditionWithoutEvents() {
        assertTrue(log.ofExpedition(PATAGONIA).isEmpty());
    }

    @Test
    void recordsTheEventOfAnExpeditionThatIsNotStored() {
        record("ev-1", PATAGONIA, ExpeditionApproved.class, OCCURRED_AT);

        assertEquals(List.of(new EventId("ev-1")), ids(log.ofExpedition(PATAGONIA)));
    }

    @Test
    void tellsWhatHappenedWhenItHappenedAndWhenItWasRecorded() {
        record("ev-1", PATAGONIA, IncidentRegistered.class, OCCURRED_AT);

        final var recorded = log.ofExpedition(PATAGONIA).getFirst();

        assertEquals(new EventId("ev-1"), recorded.id());
        assertEquals(IncidentRegistered.class.getName(), recorded.type());
        assertEquals(OCCURRED_AT, recorded.occurredAt());
        assertEquals(RECORDED_AT, recorded.recordedAt());
    }

    @Test
    void listsTheEventsInTheOrderTheyWereRecordedAndNotInTheOrderTheyHappened() {
        record("ev-1", PATAGONIA, ActivityCompleted.class, OCCURRED_AT.plusHours(5));
        record("ev-2", PATAGONIA, IncidentRegistered.class, OCCURRED_AT);

        assertEquals(List.of(new EventId("ev-1"), new EventId("ev-2")), ids(log.ofExpedition(PATAGONIA)));
    }

    @Test
    void givesEachEventTheNextSequence() {
        final var first = record("ev-1", PATAGONIA, ExpeditionApproved.class, OCCURRED_AT);
        final var second = record("ev-2", NIGHT_SURVEY, ExpeditionApproved.class, OCCURRED_AT);

        assertTrue(first < second);
    }

    @Test
    void leavesOutTheEventsOfAnotherExpedition() {
        record("ev-1", PATAGONIA, ExpeditionApproved.class, OCCURRED_AT);
        record("ev-2", NIGHT_SURVEY, ExpeditionApproved.class, OCCURRED_AT);

        assertEquals(List.of(new EventId("ev-1")), ids(log.ofExpedition(PATAGONIA)));
    }

    @Test
    void tellsHowTheEventWentForEachConsumer() {
        final var sequence = record("ev-1", PATAGONIA, ExpeditionApproved.class, OCCURRED_AT);
        deliver(sequence, PROGRESS, DeliveryStatus.FAILED, 5, "the progress of e-1 could not be updated");
        deliver(sequence, NOTIFICATIONS, DeliveryStatus.DELIVERED, 1, null);

        assertEquals(List.of(
                        new EventDeliveryView(NOTIFICATIONS, DeliveryStatus.DELIVERED, 1, Optional.empty()),
                        new EventDeliveryView(PROGRESS, DeliveryStatus.FAILED, 5,
                                Optional.of("the progress of e-1 could not be updated"))),
                log.ofExpedition(PATAGONIA).getFirst().deliveries());
    }

    @Test
    void listsAnEventThatNobodyHasToReceive() {
        record("ev-1", PATAGONIA, ExpeditionApproved.class, OCCURRED_AT);

        assertTrue(log.ofExpedition(PATAGONIA).getFirst().deliveries().isEmpty());
    }

    @Test
    void identifiesEachDeliveryByItsEventAndItsConsumer() {
        final var sequence = record("ev-1", PATAGONIA, ExpeditionApproved.class, OCCURRED_AT);
        deliver(sequence, NOTIFICATIONS, DeliveryStatus.PENDING, 0, null);

        final var delivery = deliveries.findById(new EventDeliveryKey(sequence, NOTIFICATIONS.value())).orElseThrow();

        assertEquals(DeliveryStatus.PENDING.name(), delivery.getStatus());
    }

    @Test
    void keepsThePayloadOfTheEventAsJson() {
        final var sequence = record("ev-1", PATAGONIA, ExpeditionApproved.class, OCCURRED_AT);

        final var stored = events.findById(sequence).orElseThrow().getPayload();

        assertEquals(json.readTree(PAYLOAD), json.readTree(stored));
    }

    @Test
    void rejectsASecondEventWithTheSameId() {
        record("ev-1", PATAGONIA, ExpeditionApproved.class, OCCURRED_AT);

        assertThrows(DataIntegrityViolationException.class,
                () -> record("ev-1", NIGHT_SURVEY, ActivityCompleted.class, OCCURRED_AT));
    }

    private Long record(String id, ExpeditionId expedition, Class<?> type, LocalDateTime occurredAt) {
        final var entity = new DomainEventEntity();
        entity.setEventId(id);
        entity.setExpeditionId(expedition.value());
        entity.setEventType(type.getName());
        entity.setOccurredAt(occurredAt);
        entity.setRecordedAt(RECORDED_AT);
        entity.setPayload(PAYLOAD);
        return events.saveAndFlush(entity).getSequence();
    }

    private void deliver(Long sequence, ConsumerName consumer, DeliveryStatus status, int attempts, String lastError) {
        final var entity = new EventDeliveryEntity();
        entity.setId(new EventDeliveryKey(sequence, consumer.value()));
        entity.setStatus(status.name());
        entity.setAttempts(attempts);
        entity.setLastError(lastError);
        entity.setNextAttemptAt(RECORDED_AT);
        deliveries.saveAndFlush(entity);
    }

    private List<EventId> ids(List<RecordedEventView> recorded) {
        return recorded.stream().map(RecordedEventView::id).toList();
    }
}
