package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;
import java.util.Map;

public record ScheduleActivityRequest(String id, String name, PeriodDto timeWindow, PeriodDto scheduledPeriod,
                                      String zoneId, List<String> dependencyIds, String type,
                                      Map<String, Integer> parameters, boolean atNight) {
}
