package ar.edu.itba.dps.fieldops.adapters.driven;

import ar.edu.itba.dps.fieldops.business.interfaces.events.DomainEvent;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.events.ActivityCompleted;
import ar.edu.itba.dps.fieldops.business.models.events.EventId;
import ar.edu.itba.dps.fieldops.business.models.events.ExpeditionApproved;
import ar.edu.itba.dps.fieldops.business.models.events.IncidentRegistered;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentId;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventSerializerTest {
    private static final EventId EVENT = new EventId("ev-1");
    private static final ExpeditionId PATAGONIA = new ExpeditionId("e-1");
    private static final ActivityId SAMPLING = new ActivityId("a-08");
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 2, 1, 8, 30);

    private final EventSerializer serializer = new EventSerializer();

    @Test
    void namesTheTypeOfAnEventAfterItsClass() {
        assertEquals(IncidentRegistered.class.getName(), serializer.typeOf(incident(Optional.empty(), Optional.empty())));
    }

    @Test
    void readsBackTheApprovalOfAnExpedition() {
        assertReadsBack(new ExpeditionApproved(EVENT, PATAGONIA, OCCURRED_AT));
    }

    @Test
    void readsBackTheCompletionOfAnActivity() {
        assertReadsBack(new ActivityCompleted(EVENT, PATAGONIA, SAMPLING, OCCURRED_AT, "Twelve samples collected"));
    }

    @Test
    void readsBackAnIncidentWithItsResourceAndItsDelay() {
        assertReadsBack(incident(Optional.of(new ResourceId("r-3")), Optional.of(Duration.ofMinutes(90))));
    }

    @Test
    void readsBackAnIncidentWithoutResourceOrDelay() {
        assertReadsBack(incident(Optional.empty(), Optional.empty()));
    }

    @Test
    void failsForATypeThatDoesNotExist() {
        final var payload = serializer.serialize(new ExpeditionApproved(EVENT, PATAGONIA, OCCURRED_AT));

        final var unknown = assertThrows(UnknownEventTypeException.class,
                () -> serializer.deserialize("ar.edu.itba.dps.fieldops.business.models.events.ExpeditionCancelled", payload));

        assertTrue(unknown.getMessage().contains("ExpeditionCancelled"));
        assertTrue(unknown.getMessage().contains("renamed or moved"));
    }

    @Test
    void failsForATypeThatIsNotADomainEvent() {
        final var unknown = assertThrows(UnknownEventTypeException.class,
                () -> serializer.deserialize(ExpeditionId.class.getName(), "{\"value\":\"e-1\"}"));

        assertTrue(unknown.getMessage().contains("is not a domain event"));
    }

    private void assertReadsBack(DomainEvent event) {
        assertEquals(event, serializer.deserialize(serializer.typeOf(event), serializer.serialize(event)));
    }

    private IncidentRegistered incident(Optional<ResourceId> resource, Optional<Duration> delay) {
        return new IncidentRegistered(EVENT, PATAGONIA,
                new Incident(new IncidentId(PATAGONIA, 1), SAMPLING, "A storm flooded the station", OCCURRED_AT,
                        resource, delay));
    }
}
