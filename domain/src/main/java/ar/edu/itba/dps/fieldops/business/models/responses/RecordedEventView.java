package ar.edu.itba.dps.fieldops.business.models.responses;

import ar.edu.itba.dps.fieldops.business.models.events.EventId;

import java.time.LocalDateTime;
import java.util.List;

public record RecordedEventView(EventId id, String type, LocalDateTime occurredAt, LocalDateTime recordedAt,
                                List<EventDeliveryView> deliveries) {
}
