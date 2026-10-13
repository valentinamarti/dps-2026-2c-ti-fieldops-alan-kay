package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.AssignResourceRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.AssignmentCandidatesResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.AssignmentCandidatesMapper;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.usecases.AssignResources;
import ar.edu.itba.dps.fieldops.business.usecases.FindAssignmentCandidates;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/expeditions/{expeditionId}/activities/{activityId}")
@Tag(name = "Assignment", description = "Assigning resources to a scheduled activity and its candidates")
public class AssignmentController {
    private final AssignResources assignResources;
    private final FindAssignmentCandidates findAssignmentCandidates;

    public AssignmentController(AssignResources assignResources, FindAssignmentCandidates findAssignmentCandidates) {
        this.assignResources = Objects.requireNonNull(assignResources, "assignResources is required");
        this.findAssignmentCandidates =
                Objects.requireNonNull(findAssignmentCandidates, "findAssignmentCandidates is required");
    }

    @PostMapping("/staff")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Assigns a person of the catalog to a scheduled activity")
    @ApiResponse(responseCode = "204", description = "Assigned; read it back through the itinerary")
    @ApiResponse(responseCode = "400", description = "The activity already started and cannot change")
    @ApiResponse(responseCode = "404", description = "The expedition, the activity or the person does not exist")
    @ApiResponse(responseCode = "409", description = "The expedition is not in draft anymore")
    public void assignStaff(@PathVariable String expeditionId, @PathVariable String activityId,
                            @RequestBody AssignResourceRequest request) {
        assignResources.assignStaff(new ExpeditionId(expeditionId), new ActivityId(activityId),
                new ResourceId(request.resourceId()));
    }

    @PostMapping("/equipment")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Assigns a vehicle or an instrument of the catalog to a scheduled activity")
    @ApiResponse(responseCode = "204", description = "Assigned; read it back through the itinerary")
    @ApiResponse(responseCode = "400", description = "The activity already started and cannot change")
    @ApiResponse(responseCode = "404", description = "The expedition, the activity or the equipment does not exist")
    @ApiResponse(responseCode = "409", description = "The expedition is not in draft anymore")
    public void assignEquipment(@PathVariable String expeditionId, @PathVariable String activityId,
                                @RequestBody AssignResourceRequest request) {
        assignResources.assignEquipment(new ExpeditionId(expeditionId), new ActivityId(activityId),
                new ResourceId(request.resourceId()));
    }

    @PostMapping("/supplies")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Assigns a consumable of the catalog to a scheduled activity")
    @ApiResponse(responseCode = "204", description = "Assigned; read it back through the itinerary")
    @ApiResponse(responseCode = "400", description = "The activity already started and cannot change")
    @ApiResponse(responseCode = "404", description = "The expedition, the activity or the supply does not exist")
    @ApiResponse(responseCode = "409", description = "The expedition is not in draft anymore")
    public void assignSupply(@PathVariable String expeditionId, @PathVariable String activityId,
                             @RequestBody AssignResourceRequest request) {
        assignResources.assignSupply(new ExpeditionId(expeditionId), new ActivityId(activityId),
                new ResourceId(request.resourceId()));
    }

    @GetMapping("/candidates")
    @Operation(summary = "What the catalog can offer for each requirement of a scheduled activity")
    @ApiResponse(responseCode = "200", description = "The requirements, each with the candidates that qualify for it")
    @ApiResponse(responseCode = "404", description = "The expedition or the activity does not exist")
    public AssignmentCandidatesResponse candidates(@PathVariable String expeditionId,
                                                   @PathVariable String activityId) {
        return AssignmentCandidatesMapper.toResponse(
                findAssignmentCandidates.candidatesFor(new ExpeditionId(expeditionId), new ActivityId(activityId)));
    }
}
