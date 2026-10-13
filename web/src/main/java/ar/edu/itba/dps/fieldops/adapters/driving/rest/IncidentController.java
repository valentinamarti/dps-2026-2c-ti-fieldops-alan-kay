package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.RegisterIncidentRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ReplanProposalResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.ReplanningMapper;
import ar.edu.itba.dps.fieldops.business.usecases.RegisterIncident;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/expeditions/{expeditionId}/incidents")
@Tag(name = "Incidents", description = "Registering what went wrong with a scheduled activity, which proposes a replanning")
public class IncidentController {
    private final RegisterIncident registerIncident;

    public IncidentController(RegisterIncident registerIncident) {
        this.registerIncident = Objects.requireNonNull(registerIncident, "registerIncident is required");
    }

    @PostMapping
    @Operation(summary = "Registers an incident and answers with the replan proposal it generated")
    @ApiResponse(responseCode = "201", description = "Registered, with the proposal that was generated and its "
            + "address in the Location header")
    @ApiResponse(responseCode = "400", description = "A field is missing, the expedition is not in execution, "
            + "the activity already finished, or the resource is not assigned to it")
    @ApiResponse(responseCode = "404", description = "The expedition or the scheduled activity does not exist")
    @ApiResponse(responseCode = "409", description = "There is already a proposal for this incident")
    public ResponseEntity<ReplanProposalResponse> register(@PathVariable String expeditionId,
                                                           @RequestBody RegisterIncidentRequest request) {
        final var proposal = registerIncident.register(ReplanningMapper.toCommand(expeditionId, request));
        return ResponseEntity.created(Locations.replanProposal(proposal.getId().value()))
                .body(ReplanningMapper.toResponse(proposal));
    }
}
