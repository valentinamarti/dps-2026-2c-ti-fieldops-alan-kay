package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.time.LocalDateTime;

public record FinishActivityRequest(LocalDateTime at, String result) {
}
