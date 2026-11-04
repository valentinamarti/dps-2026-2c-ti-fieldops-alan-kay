package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import ar.edu.itba.dps.fieldops.business.models.events.DeliveryStatus;

public record EventDeliveryDto(String consumer, DeliveryStatus status, int attempts, String lastError) {
}
