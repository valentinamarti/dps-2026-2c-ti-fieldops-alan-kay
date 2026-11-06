package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.EventDeliveryDto;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.RecordedEventResponse;
import ar.edu.itba.dps.fieldops.business.models.responses.EventDeliveryView;
import ar.edu.itba.dps.fieldops.business.models.responses.RecordedEventView;

public final class EventLogMapper {
    private EventLogMapper() {
    }

    public static RecordedEventResponse toResponse(RecordedEventView event) {
        return new RecordedEventResponse(
                event.id().value(),
                simpleNameOf(event.type()),
                event.occurredAt(),
                event.recordedAt(),
                event.deliveries().stream().map(EventLogMapper::toDto).toList());
    }

    private static EventDeliveryDto toDto(EventDeliveryView delivery) {
        return new EventDeliveryDto(
                delivery.consumer().value(),
                delivery.status(),
                delivery.attempts(),
                delivery.lastError().orElse(null));
    }

    private static String simpleNameOf(String type) {
        return type.substring(type.lastIndexOf('.') + 1);
    }
}
