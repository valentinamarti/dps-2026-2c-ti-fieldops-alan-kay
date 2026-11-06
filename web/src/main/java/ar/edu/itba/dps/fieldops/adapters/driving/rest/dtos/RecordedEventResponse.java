package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.time.LocalDateTime;
import java.util.List;

public record RecordedEventResponse(String id, String type, LocalDateTime occurredAt, LocalDateTime recordedAt,
                                    List<EventDeliveryDto> deliveries) {
}
