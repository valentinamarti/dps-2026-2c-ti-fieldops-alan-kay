package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.time.LocalDateTime;

public record ProgressEntryResponse(String activityId, LocalDateTime occurredAt, int finishedActivities,
                                    int scheduledActivities) {
}
