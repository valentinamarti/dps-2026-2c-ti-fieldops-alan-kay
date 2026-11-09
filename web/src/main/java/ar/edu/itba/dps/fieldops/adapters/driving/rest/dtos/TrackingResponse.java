package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import ar.edu.itba.dps.fieldops.business.models.responses.TrackingStatus;

import java.time.LocalDateTime;
import java.util.List;

public record TrackingResponse(TrackingStatus status, LocalDateTime startedAt, LocalDateTime finishedAt, String result,
                               List<ObservationDto> observations) {
}
