package ar.edu.itba.dps.fieldops.business.integration;

import ar.edu.itba.dps.fieldops.EventDispatchScheduler;
import ar.edu.itba.dps.fieldops.business.models.events.DeliveryStatus;
import ar.edu.itba.dps.fieldops.business.models.responses.EventDeliveryView;
import ar.edu.itba.dps.fieldops.business.providers.EventLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

import java.time.Duration;
import java.util.Optional;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "fieldops.events.dispatcher-enabled=true",
        "fieldops.events.dispatch-interval=200ms"
})
@Import(DispatcherTestConfiguration.class)
@Sql("/clean-database.sql")
@DirtiesContext
class ScheduledDispatchTest {
    @Autowired
    private ApprovedExpeditions approvedExpeditions;
    @Autowired
    private EventLog eventLog;
    @Autowired
    private RecordingConsumer recorder;
    @Autowired
    private ApplicationContext context;

    @Test
    void theSchedulerDeliversWhatAnOperationLeftPendingWithoutAnyoneCallingTheDispatcher() {
        assertNotNull(context.getBeanProvider(EventDispatchScheduler.class).getIfAvailable());

        final var expedition = approvedExpeditions.approveOne();
        final var approval = eventLog.ofExpedition(expedition).getFirst();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertEquals(new EventDeliveryView(RecordingConsumer.NAME, DeliveryStatus.DELIVERED, 1, Optional.empty()),
                    eventLog.ofExpedition(expedition).getFirst().deliveries().stream()
                            .filter(delivery -> delivery.consumer().equals(RecordingConsumer.NAME))
                            .findFirst().orElseThrow());
            assertTrue(recorder.received().contains(approval.id()));
        });
    }
}
