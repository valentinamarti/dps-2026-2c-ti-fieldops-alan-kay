package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ScheduleActivityRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ScheduledActivityResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.ActivityMapper;
import ar.edu.itba.dps.fieldops.business.usecases.ScheduleActivity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * The activities of the itinerary of an expedition. It only translates: the request becomes a command, the use
 * case resolves the zone, the dependencies and the rules of the type, and the scheduled activity becomes a DTO
 * (D49, D53).
 *
 * <p>The answer has no {@code Location} header: what is created is a node of the itinerary, and the itinerary
 * does not have its endpoint yet, so there would be nothing behind that address.
 */
@RestController
@RequestMapping("/api/expeditions/{expeditionId}/activities")
@Tag(name = "Itinerary", description = "The activities scheduled in an expedition")
public class ActivityController {

    private final ScheduleActivity scheduleActivity;

    public ActivityController(ScheduleActivity scheduleActivity) {
        this.scheduleActivity = Objects.requireNonNull(scheduleActivity, "scheduleActivity is required");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Schedules an activity of a type in the itinerary of an expedition")
    @ApiResponse(responseCode = "201", description = "Scheduled, with what its rules demand of it")
    @ApiResponse(responseCode = "400", description = "A field is missing, the type does not exist, a parameter "
            + "does not serve it or the schedule does not fit the plan")
    @ApiResponse(responseCode = "404", description = "The expedition, the zone or a dependency does not exist")
    @ApiResponse(responseCode = "409", description = "The expedition is not in draft anymore")
    public ScheduledActivityResponse schedule(@PathVariable String expeditionId,
                                              @RequestBody ScheduleActivityRequest request) {
        return ActivityMapper.toResponse(scheduleActivity.schedule(ActivityMapper.toCommand(expeditionId, request)));
    }
}
