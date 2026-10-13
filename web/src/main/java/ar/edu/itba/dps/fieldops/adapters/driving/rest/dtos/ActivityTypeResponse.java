package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

public record ActivityTypeResponse(String name, List<String> parameterNames) {
}
