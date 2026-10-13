package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

public record ScheduleBlockRequest(String type, List<ScheduleBlockRequest> children,
                                   ScheduleActivityRequest activity) {
}
