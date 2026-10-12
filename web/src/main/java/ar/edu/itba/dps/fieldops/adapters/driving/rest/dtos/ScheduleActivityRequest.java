package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;
import java.util.Map;

/**
 * The body of {@code POST /api/expeditions/{id}/activities}. The activity is described and not built: the zone
 * and the dependencies travel as ids and the behaviour is asked for with the name of a type and its
 * parameters, which are the ones {@code GET /api/activity-types} publishes (D53).
 *
 * @param timeWindow     the window the activity has to be scheduled within
 * @param dependencyIds  the activities of the same expedition that have to finish before this one starts
 * @param atNight        whether it is the night variant, which demands lighting, the night certification of
 *                       everyone assigned and the special permit of the zone (D42)
 */
public record ScheduleActivityRequest(String id, String name, PeriodDto timeWindow, PeriodDto scheduledPeriod,
                                      String zoneId, List<String> dependencyIds, String type,
                                      Map<String, Integer> parameters, boolean atNight) {
}
