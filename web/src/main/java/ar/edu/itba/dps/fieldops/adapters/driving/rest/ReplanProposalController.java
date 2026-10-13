package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.AcceptReplanOptionRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ExpeditionResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ReplanDecisionRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ReplanProposalResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.ExpeditionMapper;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.ReplanningMapper;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalId;
import ar.edu.itba.dps.fieldops.business.usecases.DecideReplanProposal;
import ar.edu.itba.dps.fieldops.business.usecases.QueryReplanProposals;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * The replan proposals: reading one, reading the ones of an expedition, and deciding on one (D47, D48, D58).
 * A proposal has an id of its own and an address of its own, which is the one the incident that generated it
 * points at, so it is a top-level resource; listing them by expedition is the same resource under its parent,
 * which is why both paths live in this controller (D49's one controller per resource).
 *
 * <p>Deciding is a sub-resource in {@code POST}, like every other operation that is not CRUD (D49):
 * {@code POST .../acceptance} and {@code POST .../rejection}. The two are not symmetric in what they answer,
 * because they are not symmetric in what they do: accepting builds the alternative as a draft and returns it,
 * with its address in the {@code Location}; rejecting changes nothing in the expedition, so it answers 204
 * like the rest of the writes of the API (D52, D56, D57), and the decision is read back from the proposal.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Replan proposals", description = "Reading the replanning proposed after an incident and deciding on it")
public class ReplanProposalController {

    private final QueryReplanProposals queryReplanProposals;
    private final DecideReplanProposal decideReplanProposal;

    public ReplanProposalController(QueryReplanProposals queryReplanProposals, DecideReplanProposal decideReplanProposal) {
        this.queryReplanProposals = Objects.requireNonNull(queryReplanProposals, "queryReplanProposals is required");
        this.decideReplanProposal = Objects.requireNonNull(decideReplanProposal, "decideReplanProposal is required");
    }

    @GetMapping("/expeditions/{expeditionId}/replan-proposals")
    @Operation(summary = "Lists the replan proposals of an expedition, decided or not")
    @ApiResponse(responseCode = "200", description = "The proposals, in the order they were made")
    @ApiResponse(responseCode = "404", description = "There is no expedition with that id")
    public List<ReplanProposalResponse> ofExpedition(@PathVariable String expeditionId) {
        return queryReplanProposals.ofExpedition(new ExpeditionId(expeditionId)).stream()
                .map(ReplanningMapper::toResponse)
                .toList();
    }

    @GetMapping("/replan-proposals/{proposalId}")
    @Operation(summary = "Gets one replan proposal by id, with its incident, its options and its decision")
    @ApiResponse(responseCode = "200", description = "The proposal, at any status")
    @ApiResponse(responseCode = "404", description = "There is no replan proposal with that id")
    public ReplanProposalResponse get(@PathVariable String proposalId) {
        return ReplanningMapper.toResponse(queryReplanProposals.get(new ReplanProposalId(proposalId)));
    }

    @PostMapping("/replan-proposals/{proposalId}/acceptance")
    @Operation(summary = "Accepts one option of a proposal, which creates the alternative expedition as a draft")
    @ApiResponse(responseCode = "201", description = "Accepted, with the alternative in draft and its address in "
            + "the Location header. It reaches the original it replaces through replacedExpeditionId")
    @ApiResponse(responseCode = "400", description = "A field is missing, or the option can no longer be planned")
    @ApiResponse(responseCode = "404", description = "The proposal, the option or the responsible does not exist")
    @ApiResponse(responseCode = "409", description = "The proposal was already decided, the original cannot be "
            + "replaced, or an activity is still in progress")
    @ApiResponse(responseCode = "422", description = "Whoever decides is not a responsible of the expedition")
    public ResponseEntity<ExpeditionResponse> accept(@PathVariable String proposalId,
                                                     @RequestBody AcceptReplanOptionRequest request) {
        final var alternative = decideReplanProposal.accept(new ReplanProposalId(proposalId), request.optionNumber(),
                ReplanningMapper.toCommand(request));
        final var response = ExpeditionMapper.toResponse(alternative);
        return ResponseEntity.created(Locations.expedition(response.id())).body(response);
    }

    @PostMapping("/replan-proposals/{proposalId}/rejection")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Rejects a proposal, leaving the expedition running with the plan it had")
    @ApiResponse(responseCode = "204", description = "Rejected; read the decision back from GET .../replan-proposals/{id}")
    @ApiResponse(responseCode = "400", description = "A field is missing")
    @ApiResponse(responseCode = "404", description = "The proposal or the responsible does not exist")
    @ApiResponse(responseCode = "409", description = "The proposal was already decided")
    @ApiResponse(responseCode = "422", description = "Whoever decides is not a responsible of the expedition")
    public void reject(@PathVariable String proposalId, @RequestBody ReplanDecisionRequest request) {
        decideReplanProposal.reject(new ReplanProposalId(proposalId), ReplanningMapper.toCommand(request));
    }
}
