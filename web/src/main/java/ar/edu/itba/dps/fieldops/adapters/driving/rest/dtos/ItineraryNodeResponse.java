package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ItineraryNodeResponse(String type, PeriodDto scheduledPeriod, long estimatedDurationMinutes,
                                    RiskLevel estimatedRisk, ScheduledActivityResponse activity,
                                    List<ItineraryNodeResponse> children) {
}
