package ar.edu.itba.dps.fieldops.business.integration;

import ar.edu.itba.dps.fieldops.adapters.driven.EventDispatcher;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.events.ConsumerName;
import ar.edu.itba.dps.fieldops.business.models.events.DeliveryStatus;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.report.ProgressEntry;
import ar.edu.itba.dps.fieldops.business.models.responses.EventDeliveryView;
import ar.edu.itba.dps.fieldops.business.ports.in.QueryNotificationsPort;
import ar.edu.itba.dps.fieldops.business.ports.in.ReportExpeditionPort;
import ar.edu.itba.dps.fieldops.business.ports.in.TrackActivityPort;
import ar.edu.itba.dps.fieldops.business.ports.in.TransitionExpeditionPort;
import ar.edu.itba.dps.fieldops.business.providers.EventLog;
import ar.edu.itba.dps.fieldops.business.providers.ProgressHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@Import(DispatcherTestConfiguration.class)
@Sql("/clean-database.sql")
class AFailingProgressConsumerTest {
    private static final ActivityId ACTIVITY = new ActivityId("a-1");
    private static final LocalDateTime THIRTEEN = LocalDateTime.of(2026, 10, 1, 13, 0);
    private static final ConsumerName TRACK_PROGRESS = new ConsumerName("track-progress");
    private static final ConsumerName NOTIFY_RESPONSIBLES = new ConsumerName("notify-responsibles");
    private static final String FAILURE = "the progress could not be saved";
    private static final int MORE_RUNS_THAN_ATTEMPTS = 6;

    @MockitoBean
    private ProgressHistory history;
    @Autowired
    private ApprovedExpeditions approvedExpeditions;
    @Autowired
    private TransitionExpeditionPort transitions;
    @Autowired
    private TrackActivityPort tracking;
    @Autowired
    private EventDispatcher dispatcher;
    @Autowired
    private EventLog eventLog;
    @Autowired
    private QueryNotificationsPort notifications;
    @Autowired
    private ReportExpeditionPort reports;
    @Autowired
    private MutableClock clock;

    private ExpeditionId anExpeditionWithItsActivityFinished() {
        final var expedition = approvedExpeditions.approveOne();
        transitions.start(expedition);
        tracking.start(expedition, ACTIVITY, THIRTEEN);
        tracking.finish(expedition, ACTIVITY, THIRTEEN.plusHours(1), "Twelve samples collected");
        return expedition;
    }

    private void dispatchUntilTheRetriesRunOut() {
        for (var run = 0; run < MORE_RUNS_THAN_ATTEMPTS; run++) {
            dispatcher.dispatchPending();
            clock.advance(Duration.ofMinutes(1));
        }
    }

    private EventDeliveryView deliveryOfTheCompletionTo(ExpeditionId expedition, ConsumerName consumer) {
        return eventLog.ofExpedition(expedition).getLast().deliveries().stream()
                .filter(delivery -> delivery.consumer().equals(consumer))
                .findFirst().orElseThrow();
    }

    @Test
    void itEndsFailedWithItsErrorWithoutUndoingTheOperationNorHoldingBackTheNotifications() {
        doThrow(new IllegalStateException(FAILURE)).when(history).add(any(ProgressEntry.class));
        final var expedition = anExpeditionWithItsActivityFinished();

        dispatchUntilTheRetriesRunOut();

        final var failed = deliveryOfTheCompletionTo(expedition, TRACK_PROGRESS);
        assertEquals(DeliveryStatus.FAILED, failed.status());
        assertEquals(Optional.of(FAILURE), failed.lastError());
        assertEquals(DeliveryStatus.DELIVERED, deliveryOfTheCompletionTo(expedition, NOTIFY_RESPONSIBLES).status());
        assertEquals(2, notifications.ofExpedition(expedition).size());
        assertEquals(1, reports.report(expedition).finishedActivities());
    }
}
