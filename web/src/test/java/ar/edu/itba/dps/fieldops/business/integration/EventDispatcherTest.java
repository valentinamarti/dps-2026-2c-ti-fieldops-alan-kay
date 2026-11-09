package ar.edu.itba.dps.fieldops.business.integration;

import ar.edu.itba.dps.fieldops.EventDispatchScheduler;
import ar.edu.itba.dps.fieldops.adapters.driven.EventDispatcher;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.DomainEventEntity;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.DomainEventJpaRepository;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.EventDeliveryEntity;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.EventDeliveryJpaRepository;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.EventDeliveryKey;
import ar.edu.itba.dps.fieldops.business.interfaces.events.DomainEvent;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.events.ActivityCompleted;
import ar.edu.itba.dps.fieldops.business.models.events.ConsumerName;
import ar.edu.itba.dps.fieldops.business.models.events.DeliveryStatus;
import ar.edu.itba.dps.fieldops.business.models.events.EventId;
import ar.edu.itba.dps.fieldops.business.models.events.ExpeditionApproved;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.responses.RecordedEventView;
import ar.edu.itba.dps.fieldops.business.providers.EventLog;
import ar.edu.itba.dps.fieldops.business.providers.EventPublisher;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;

import static ar.edu.itba.dps.fieldops.business.integration.DispatcherTestConfiguration.NOON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(DispatcherTestConfiguration.class)
@Sql("/clean-database.sql")
class EventDispatcherTest {
    private static final String PENDING = DeliveryStatus.PENDING.name();
    private static final String DELIVERED = DeliveryStatus.DELIVERED.name();
    private static final String FAILED = DeliveryStatus.FAILED.name();

    @Autowired
    private EventDispatcher dispatcher;
    @Autowired
    private EventPublisher publisher;
    @Autowired
    private DomainEventJpaRepository events;
    @Autowired
    private EventDeliveryJpaRepository deliveries;
    @Autowired
    private EventLog eventLog;
    @Autowired
    private ExpeditionRepository expeditions;
    @Autowired
    private ApprovedExpeditions approvedExpeditions;
    @Autowired
    private MutableClock clock;
    @Autowired
    private RecordingConsumer recorder;
    @Autowired
    private FlakyConsumer flaky;
    @Autowired
    private ApplicationContext context;
    @Autowired
    private PlatformTransactionManager transactions;

    @BeforeEach
    void startAtNoonWithConsumersThatHaveReceivedNothing() {
        clock.setTo(NOON);
        recorder.forget();
        flaky.forget();
    }

    @Test
    void theSchedulerIsOffInTheTestsSoTheyDispatchByHand() {
        assertNull(context.getBeanProvider(EventDispatchScheduler.class).getIfAvailable());
    }

    @Test
    void deliversWhatIsPendingAndMarksItDelivered() {
        final var approval = approvalOf("e-1");
        clock.advance(Duration.ofSeconds(3));

        dispatcher.dispatchPending();

        assertEquals(List.of(approval.id()), recorder.received());
        final var delivery = deliveryOf(approval.id(), RecordingConsumer.NAME);
        assertEquals(DELIVERED, delivery.getStatus());
        assertEquals(1, delivery.getAttempts());
        assertEquals(NOON.plusSeconds(3), delivery.getDeliveredAt());
        assertNull(delivery.getLastError());
    }

    @Test
    void dispatchingAgainDoesNotRepeatTheEffect() {
        final var approval = approvalOf("e-1");

        dispatcher.dispatchPending();
        clock.advance(Duration.ofMinutes(5));
        dispatcher.dispatchPending();

        assertEquals(List.of(approval.id()), recorder.received());
        assertEquals(List.of(approval.id()), flaky.received());
        assertEquals(1, deliveryOf(approval.id(), RecordingConsumer.NAME).getAttempts());
    }

    @Test
    void aFailureAfterTheConsumerWroteLeavesNoEffectAndCountsTheAttempt() {
        final var approval = approvalOf("e-1");
        flaky.failTimes(approval.id(), 1);

        dispatcher.dispatchPending();

        assertEquals(List.of(), flaky.received());
        final var failed = deliveryOf(approval.id(), FlakyConsumer.NAME);
        assertEquals(PENDING, failed.getStatus());
        assertEquals(1, failed.getAttempts());
        assertEquals(FlakyConsumer.FAILURE, failed.getLastError());
        assertEquals(NOON.plusSeconds(1), failed.getNextAttemptAt());
        assertNull(failed.getDeliveredAt());

        clock.advance(Duration.ofSeconds(1));
        dispatcher.dispatchPending();

        assertEquals(List.of(approval.id()), flaky.received());
        assertEquals(DELIVERED, deliveryOf(approval.id(), FlakyConsumer.NAME).getStatus());
        assertEquals(2, deliveryOf(approval.id(), FlakyConsumer.NAME).getAttempts());
    }

    @Test
    void waitsTheExactBackoffBeforeEachRetry() {
        final var approval = approvalOf("e-1");
        flaky.failTimes(approval.id(), 4);
        dispatcher.dispatchPending();

        var attempts = 1;
        var nextAttempt = NOON;
        for (final var backoff : List.of(1, 2, 4, 8)) {
            nextAttempt = nextAttempt.plusSeconds(backoff);
            assertEquals(nextAttempt, deliveryOf(approval.id(), FlakyConsumer.NAME).getNextAttemptAt());

            clock.setTo(nextAttempt.minus(Duration.ofMillis(1)));
            dispatcher.dispatchPending();
            assertEquals(attempts, deliveryOf(approval.id(), FlakyConsumer.NAME).getAttempts());

            clock.setTo(nextAttempt);
            dispatcher.dispatchPending();
            attempts++;
        }

        final var delivered = deliveryOf(approval.id(), FlakyConsumer.NAME);
        assertEquals(DELIVERED, delivered.getStatus());
        assertEquals(5, delivered.getAttempts());
        assertEquals(NOON.plusSeconds(15), delivered.getDeliveredAt());
        assertEquals(List.of(approval.id()), flaky.received());
    }

    @Test
    void exhaustingTheAttemptsLeavesItFailedWithItsErrorAndHowManyAttemptsItTook() {
        final var approval = approvalOf("e-1");
        flaky.failTimes(approval.id(), 5);

        dispatchAtEachRetryOf(approval.id(), FlakyConsumer.NAME);

        final var failed = deliveryOf(approval.id(), FlakyConsumer.NAME);
        assertEquals(FAILED, failed.getStatus());
        assertEquals(5, failed.getAttempts());
        assertEquals(FlakyConsumer.FAILURE, failed.getLastError());
        assertNull(failed.getDeliveredAt());
        assertEquals(List.of(), flaky.received());

        clock.advance(Duration.ofHours(1));
        dispatcher.dispatchPending();
        assertEquals(5, deliveryOf(approval.id(), FlakyConsumer.NAME).getAttempts());
    }

    @Test
    void aFailingConsumerDoesNotUndoTheOperationNorHoldBackTheOtherConsumerOfTheSameEvent() {
        final var expedition = approvedExpeditions.approveOne();
        final var approval = eventLog.ofExpedition(expedition).getFirst().id();
        flaky.failTimes(approval, 5);

        dispatchAtEachRetryOf(approval, FlakyConsumer.NAME);

        assertEquals(ExpeditionStatus.APPROVED, expeditions.findById(expedition).orElseThrow().getStatus());
        assertEquals(List.of(ExpeditionApproved.class.getName()),
                eventLog.ofExpedition(expedition).stream().map(RecordedEventView::type).toList());
        assertEquals(FAILED, deliveryOf(approval, FlakyConsumer.NAME).getStatus());
        assertEquals(DELIVERED, deliveryOf(approval, RecordingConsumer.NAME).getStatus());
        assertEquals(List.of(approval), recorder.received());
    }

    @Test
    void theNextEventsOfThatExpeditionWaitForThatConsumerAndArriveInOrder() {
        final var approval = approvalOf("e-1");
        final var completion = completionOf("e-1");
        flaky.failTimes(approval.id(), 2);

        dispatcher.dispatchPending();

        assertEquals(List.of(), flaky.received());
        final var waiting = deliveryOf(completion.id(), FlakyConsumer.NAME);
        assertEquals(PENDING, waiting.getStatus());
        assertEquals(0, waiting.getAttempts());
        assertEquals(List.of(approval.id(), completion.id()), recorder.received());

        clock.advance(Duration.ofSeconds(1));
        dispatcher.dispatchPending();
        assertEquals(0, deliveryOf(completion.id(), FlakyConsumer.NAME).getAttempts());

        clock.advance(Duration.ofSeconds(2));
        dispatcher.dispatchPending();
        assertEquals(List.of(approval.id(), completion.id()), flaky.received());
    }

    @Test
    void theNextEventsOfThatExpeditionGoOnOnceTheOneHoldingThemBackFails() {
        final var approval = approvalOf("e-1");
        final var completion = completionOf("e-1");
        flaky.failTimes(approval.id(), 5);

        dispatchAtEachRetryOf(approval.id(), FlakyConsumer.NAME);

        assertEquals(FAILED, deliveryOf(approval.id(), FlakyConsumer.NAME).getStatus());
        assertEquals(DELIVERED, deliveryOf(completion.id(), FlakyConsumer.NAME).getStatus());
        assertEquals(List.of(completion.id()), flaky.received());
    }

    @Test
    void theEventsOfAnotherExpeditionDoNotWait() {
        final var stuck = approvalOf("e-1");
        final var other = approvalOf("e-2");
        flaky.failTimes(stuck.id(), 5);

        dispatcher.dispatchPending();

        assertEquals(List.of(other.id()), flaky.received());
        assertEquals(DELIVERED, deliveryOf(other.id(), FlakyConsumer.NAME).getStatus());
        assertEquals(PENDING, deliveryOf(stuck.id(), FlakyConsumer.NAME).getStatus());
    }

    @Test
    void aConsumerThatNoLongerExistsLeavesItsDeliveryFailedWithoutHoldingBackTheOthers() {
        final var approval = approvalOf("e-1");
        final var retired = new ConsumerName("retired-consumer");
        leavePending(sequenceOf(approval.id()), retired);

        dispatcher.dispatchPending();

        final var orphan = deliveryOf(approval.id(), retired);
        assertEquals(FAILED, orphan.getStatus());
        assertEquals(1, orphan.getAttempts());
        assertEquals("There is no async subscriber named retired-consumer, so nobody can receive this event",
                orphan.getLastError());
        assertEquals(List.of(approval.id()), recorder.received());
    }

    @Test
    void anEventThatCannotBeReadLeavesItsDeliveryFailedAndTheNextOnesGoOn() {
        final var renamed = "ar.edu.itba.dps.fieldops.business.models.events.ExpeditionRenamed";
        final var unreadable = recordUnreadable("e-1", renamed);
        leavePending(unreadable.getSequence(), RecordingConsumer.NAME);
        final var approval = approvalOf("e-1");

        dispatcher.dispatchPending();

        final var failed = deliveries.findById(new EventDeliveryKey(unreadable.getSequence(),
                RecordingConsumer.NAME.value())).orElseThrow();
        assertEquals(FAILED, failed.getStatus());
        assertEquals(1, failed.getAttempts());
        assertTrue(failed.getLastError().contains("There is no event type named " + renamed), failed.getLastError());
        assertEquals(List.of(approval.id()), recorder.received());
    }

    @Test
    void keepsTheClassOfAFailureWithoutMessageAndCutsAMessageThatDoesNotFit() {
        final var silent = approvalOf("e-1");
        final var verbose = approvalOf("e-2");
        flaky.failWith(silent.id(), new IllegalStateException());
        flaky.failWith(verbose.id(), new IllegalStateException("x".repeat(1500)));

        dispatcher.dispatchPending();

        assertEquals(IllegalStateException.class.getName(), deliveryOf(silent.id(), FlakyConsumer.NAME).getLastError());
        assertEquals("x".repeat(1000), deliveryOf(verbose.id(), FlakyConsumer.NAME).getLastError());
    }

    private ExpeditionApproved approvalOf(String expedition) {
        return published(new ExpeditionApproved(EventId.generate(), new ExpeditionId(expedition), NOON));
    }

    private ActivityCompleted completionOf(String expedition) {
        return published(new ActivityCompleted(EventId.generate(), new ExpeditionId(expedition), new ActivityId("a-1"),
                NOON, "Twelve samples"));
    }

    private <E extends DomainEvent> E published(E event) {
        new TransactionTemplate(transactions).executeWithoutResult(_ -> publisher.publish(event));
        return event;
    }

    private void dispatchAtEachRetryOf(EventId event, ConsumerName consumer) {
        dispatcher.dispatchPending();
        for (var retry = 0; retry < 10 && PENDING.equals(deliveryOf(event, consumer).getStatus()); retry++) {
            clock.setTo(deliveryOf(event, consumer).getNextAttemptAt());
            dispatcher.dispatchPending();
        }
    }

    private EventDeliveryEntity deliveryOf(EventId event, ConsumerName consumer) {
        return deliveries.findById(new EventDeliveryKey(sequenceOf(event), consumer.value())).orElseThrow();
    }

    private long sequenceOf(EventId event) {
        return events.findAll().stream()
                .filter(recorded -> recorded.getEventId().equals(event.value()))
                .findFirst().orElseThrow()
                .getSequence();
    }

    private DomainEventEntity recordUnreadable(String expedition, String type) {
        final var recorded = new DomainEventEntity();
        recorded.setEventId(EventId.generate().value());
        recorded.setExpeditionId(expedition);
        recorded.setEventType(type);
        recorded.setOccurredAt(NOON);
        recorded.setRecordedAt(NOON);
        recorded.setPayload("{}");
        return events.save(recorded);
    }

    private void leavePending(long sequence, ConsumerName consumer) {
        final var delivery = new EventDeliveryEntity();
        delivery.setId(new EventDeliveryKey(sequence, consumer.value()));
        delivery.setStatus(PENDING);
        delivery.setAttempts(0);
        delivery.setNextAttemptAt(NOON);
        deliveries.save(delivery);
    }
}
