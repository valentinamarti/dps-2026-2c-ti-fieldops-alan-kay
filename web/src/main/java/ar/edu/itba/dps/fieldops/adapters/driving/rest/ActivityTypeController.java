package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ActivityTypeResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.ActivityTypeMapper;
import ar.edu.itba.dps.fieldops.business.usecases.QueryActivityTypes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * The types of activity the domain offers. It is the endpoint that tells a client what it can ask for when it
 * schedules an activity, so it is read only: the types are decided by the domain and not registered over HTTP
 * (D44).
 */
@RestController
@RequestMapping("/api/activity-types")
@Tag(name = "Activity types", description = "The types of activity an activity can be scheduled as")
public class ActivityTypeController {

    private final QueryActivityTypes queryActivityTypes;

    public ActivityTypeController(QueryActivityTypes queryActivityTypes) {
        this.queryActivityTypes = Objects.requireNonNull(queryActivityTypes, "queryActivityTypes is required");
    }

    @GetMapping
    @Operation(summary = "Lists the types of activity with the parameters each one needs")
    @ApiResponse(responseCode = "200", description = "The types, in the order the domain registers them")
    public List<ActivityTypeResponse> list() {
        return queryActivityTypes.list().stream().map(ActivityTypeMapper::toResponse).toList();
    }
}
