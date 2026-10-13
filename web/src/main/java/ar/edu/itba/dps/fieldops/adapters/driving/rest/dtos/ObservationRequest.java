package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.time.LocalDateTime;

public record ObservationRequest(String note, LocalDateTime recordedAt) {
}
