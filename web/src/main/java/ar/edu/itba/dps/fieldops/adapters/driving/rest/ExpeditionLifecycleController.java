package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ApprovalRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ExpeditionReportResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.ApprovalMapper;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.ReportMapper;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.usecases.ApproveExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.FinishExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.ReportExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.TransitionExpedition;
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
@RequestMapping("/api/expeditions/{expeditionId}")
@Tag(name = "Lifecycle", description = "Moving an expedition through its states, approving it, closing it and reporting on it")
public class ExpeditionLifecycleController {
    private final TransitionExpedition transitionExpedition;
    private final ApproveExpedition approveExpedition;
    private final FinishExpedition finishExpedition;
    private final ReportExpedition reportExpedition;

    public ExpeditionLifecycleController(TransitionExpedition transitionExpedition, ApproveExpedition approveExpedition,
                                         FinishExpedition finishExpedition, ReportExpedition reportExpedition) {
        this.transitionExpedition = Objects.requireNonNull(transitionExpedition, "transitionExpedition is required");
        this.approveExpedition = Objects.requireNonNull(approveExpedition, "approveExpedition is required");
        this.finishExpedition = Objects.requireNonNull(finishExpedition, "finishExpedition is required");
        this.reportExpedition = Objects.requireNonNull(reportExpedition, "reportExpedition is required");
    }

    @PostMapping("/review")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Submits a draft expedition for review")
    @ApiResponse(responseCode = "204", description = "In review; read its status back from GET /api/expeditions/{id}")
    @ApiResponse(responseCode = "404", description = "There is no expedition with that id")
    @ApiResponse(responseCode = "409", description = "The expedition is not in draft")
    public void submitForReview(@PathVariable String expeditionId) {
        transitionExpedition.submitForReview(new ExpeditionId(expeditionId));
    }

    @PostMapping("/draft")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Returns an expedition in review to draft, to correct it")
    @ApiResponse(responseCode = "204", description = "Back in draft")
    @ApiResponse(responseCode = "404", description = "There is no expedition with that id")
    @ApiResponse(responseCode = "409", description = "The expedition is not in review")
    public void returnToDraft(@PathVariable String expeditionId) {
        transitionExpedition.returnToDraft(new ExpeditionId(expeditionId));
    }

    @PostMapping("/approval")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Approves an expedition in review, accepting the warnings a responsible takes on")
    @ApiResponse(responseCode = "204", description = "Approved")
    @ApiResponse(responseCode = "404", description = "The expedition, a responsible or an accepted warning does not exist")
    @ApiResponse(responseCode = "409", description = "The expedition is not in review")
    @ApiResponse(responseCode = "422",
            description = "A critical is still open, or a warning the expedition raises was not accepted")
    public void approve(@PathVariable String expeditionId, @RequestBody ApprovalRequest request) {
        approveExpedition.approve(new ExpeditionId(expeditionId), ApprovalMapper.toAcceptances(request));
    }

    @PostMapping("/execution")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Starts an approved (or suspended) expedition")
    @ApiResponse(responseCode = "204", description = "In execution")
    @ApiResponse(responseCode = "404", description = "There is no expedition with that id")
    @ApiResponse(responseCode = "409", description = "The expedition is not approved or suspended")
    public void start(@PathVariable String expeditionId) {
        transitionExpedition.start(new ExpeditionId(expeditionId));
    }

    @PostMapping("/suspension")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Suspends an expedition in execution")
    @ApiResponse(responseCode = "204", description = "Suspended")
    @ApiResponse(responseCode = "404", description = "There is no expedition with that id")
    @ApiResponse(responseCode = "409", description = "The expedition is not in execution")
    public void suspend(@PathVariable String expeditionId) {
        transitionExpedition.suspend(new ExpeditionId(expeditionId));
    }

    @PostMapping("/resumption")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Resumes a suspended expedition")
    @ApiResponse(responseCode = "204", description = "In execution again")
    @ApiResponse(responseCode = "404", description = "There is no expedition with that id")
    @ApiResponse(responseCode = "409", description = "The expedition is not suspended")
    public void resume(@PathVariable String expeditionId) {
        transitionExpedition.resume(new ExpeditionId(expeditionId));
    }

    @PostMapping("/completion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Finishes an expedition in execution and releases what it still had reserved")
    @ApiResponse(responseCode = "204", description = "Finished; read the outcome from GET .../report")
    @ApiResponse(responseCode = "404", description = "There is no expedition with that id")
    @ApiResponse(responseCode = "409", description = "The expedition is not in execution, or an activity is still in progress")
    public void finish(@PathVariable String expeditionId) {
        finishExpedition.finish(new ExpeditionId(expeditionId));
    }

    @GetMapping("/report")
    @Operation(summary = "What the expedition is worth: its plan, its actual consumption, its incidents and its accepted warnings")
    @ApiResponse(responseCode = "200", description = "The report, at any status")
    @ApiResponse(responseCode = "404", description = "There is no expedition with that id")
    public ExpeditionReportResponse report(@PathVariable String expeditionId) {
        return ReportMapper.toResponse(reportExpedition.report(new ExpeditionId(expeditionId)));
    }
}
