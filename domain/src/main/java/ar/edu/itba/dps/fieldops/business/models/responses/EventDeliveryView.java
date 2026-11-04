package ar.edu.itba.dps.fieldops.business.models.responses;

import ar.edu.itba.dps.fieldops.business.models.events.ConsumerName;
import ar.edu.itba.dps.fieldops.business.models.events.DeliveryStatus;

import java.util.Optional;

public record EventDeliveryView(ConsumerName consumer, DeliveryStatus status, int attempts,
                                Optional<String> lastError) {
}
