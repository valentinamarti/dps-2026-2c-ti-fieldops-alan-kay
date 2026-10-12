package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * A node of the itinerary of an expedition (D54). Every node answers the same four things, because that is
 * what the itinerary asks of any node of its tree: when it happens, how long it takes, how risky it is and of
 * what kind it is. A leaf adds its {@code activity}, with the same body the schedule of an activity returns
 * (D53), and a block adds its {@code children}.
 *
 * <p>The field that does not apply is left out instead of travelling as {@code null}: a leaf has no children
 * and a block has no activity of its own.
 *
 * @param type                     {@code activity}, {@code sequential} or {@code parallel}
 * @param estimatedDurationMinutes the duration the node estimates, in minutes, which in a sequential block is
 *                                 the sum of its parts and in a parallel one the maximum (D39)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ItineraryNodeResponse(String type, PeriodDto scheduledPeriod, long estimatedDurationMinutes,
                                    RiskLevel estimatedRisk, ScheduledActivityResponse activity,
                                    List<ItineraryNodeResponse> children) {
}
