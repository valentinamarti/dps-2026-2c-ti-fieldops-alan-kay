package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.time.LocalDateTime;

public record NotificationResponse(String eventId, String responsibleId, String text, LocalDateTime occurredAt) {
}
