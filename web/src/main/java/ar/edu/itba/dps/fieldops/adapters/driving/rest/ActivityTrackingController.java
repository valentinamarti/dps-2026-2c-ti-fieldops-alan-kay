package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.FinishActivityRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ObservationRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.StartActivityRequest;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Observation;
import ar.edu.itba.dps.fieldops.business.usecases.TrackActivity;
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
 * Tracking a scheduled activity while the expedition executes it: when it started, what was observed along
 * the way and when and how it finished (D49). It shares the base path of {@link AssignmentController} because
 * both operate on the same scheduled activity, but it is a separate controller because tracking is a
 * different concern from assigning resources to it (D49's one-controller-per-resource, applied to two
 * concerns of the same sub-resource, the same split {@code ActivityController} and {@code AssignmentController}
 * already have).
 *
 * <p>The three operations answer 204: {@code TrackActivity} returns {@code void}, and what they changed is
 * read back through {@code GET /api/expeditions/{id}/report} (finishing changes the actual consumption and
 * the finished count) rather than through an object of its own.
 */
@RestController
@RequestMapping("/api/expeditions/{expeditionId}/activities/{activityId}")
@Tag(name = "Tracking", description = "Tracking a scheduled activity while the expedition is in execution")
public class ActivityTrackingController {

    private final TrackActivity trackActivity;

    public ActivityTrackingController(TrackActivity trackActivity) {
        this.trackActivity = Objects.requireNonNull(trackActivity, "trackActivity is required");
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Records when a scheduled activity actually started")
    @ApiResponse(responseCode = "204", description = "Started")
    @ApiResponse(responseCode = "404", description = "The expedition or the scheduled activity does not exist")
    @ApiResponse(responseCode = "400", description = "The expedition is not in execution, or the activity already started")
    public void start(@PathVariable String expeditionId, @PathVariable String activityId,
                      @RequestBody StartActivityRequest request) {
        trackActivity.start(new ExpeditionId(expeditionId), new ActivityId(activityId), request.at());
    }

    @PostMapping("/completion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Records when a scheduled activity finished and its actual result, consuming what it used")
    @ApiResponse(responseCode = "204", description = "Finished; read the outcome from GET .../report")
    @ApiResponse(responseCode = "404", description = "The expedition or the scheduled activity does not exist")
    @ApiResponse(responseCode = "400", description = "The expedition is not in execution, or the activity did not start yet")
    @ApiResponse(responseCode = "422", description = "A supply it needs does not have enough stock left")
    public void finish(@PathVariable String expeditionId, @PathVariable String activityId,
                       @RequestBody FinishActivityRequest request) {
        trackActivity.finish(new ExpeditionId(expeditionId), new ActivityId(activityId), request.at(), request.result());
    }

    @PostMapping("/observations")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Records a note about a scheduled activity while it is tracked")
    @ApiResponse(responseCode = "204", description = "Recorded")
    @ApiResponse(responseCode = "404", description = "The expedition or the scheduled activity does not exist")
    @ApiResponse(responseCode = "400", description = "The expedition is not in execution")
    public void observe(@PathVariable String expeditionId, @PathVariable String activityId,
                        @RequestBody ObservationRequest request) {
        trackActivity.observe(new ExpeditionId(expeditionId), new ActivityId(activityId),
                new Observation(request.note(), request.recordedAt()));
    }
}
