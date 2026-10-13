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

/**
 * What goes wrong in the field, registered against a scheduled activity of an expedition in execution (D48,
 * D58). It only translates: the report becomes a command and the use case does the work.
 *
 * <p>This is the one write of the API that has something of its own to answer with, and it is what makes the
 * first acceptance criterion of F3 observable over HTTP: delivery of {@code IncidentRegistered} is synchronous,
 * so by the time {@code RegisterIncident} returns the proposal already exists (D45), and it comes back in the
 * body with its options. The {@code Location} points at the proposal, which is the resource that was created
 * and the one the decision endpoints work on; the incident itself is read back from
 * {@code GET /api/expeditions/{id}/report}, which lists the whole log (D24, D48).
 */
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
