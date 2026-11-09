package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.time.LocalDateTime;

public record ObservationDto(String note, LocalDateTime recordedAt) {
}
