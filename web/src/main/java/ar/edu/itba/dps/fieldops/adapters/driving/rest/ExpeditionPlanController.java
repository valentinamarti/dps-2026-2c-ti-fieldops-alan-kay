package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.EstimateResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ValidationResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.EstimateMapper;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.ValidationMapper;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.usecases.EstimateExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.ValidateExpedition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/expeditions/{expeditionId}")
@Tag(name = "Validation and estimation", description = "Validating and estimating an expedition without approving it")
public class ExpeditionPlanController {
    private final ValidateExpedition validateExpedition;
    private final EstimateExpedition estimateExpedition;

    public ExpeditionPlanController(ValidateExpedition validateExpedition, EstimateExpedition estimateExpedition) {
        this.validateExpedition = Objects.requireNonNull(validateExpedition, "validateExpedition is required");
        this.estimateExpedition = Objects.requireNonNull(estimateExpedition, "estimateExpedition is required");
    }

    @GetMapping("/validation")
    @Operation(summary = "Runs the approval rules without approving, with the criticals and the warnings apart")
    @ApiResponse(responseCode = "200", description = "What the rules found, and whether it can be approved")
    @ApiResponse(responseCode = "404", description = "There is no expedition with that id")
    public ValidationResponse validate(@PathVariable String expeditionId) {
        return ValidationMapper.toResponse(validateExpedition.validate(new ExpeditionId(expeditionId)));
    }

    @GetMapping("/estimate")
    @Operation(summary = "Estimates the duration, the risk and the consumption of an expedition")
    @ApiResponse(responseCode = "200", description = "The estimate of its itinerary")
    @ApiResponse(responseCode = "404", description = "There is no expedition with that id")
    public EstimateResponse estimate(@PathVariable String expeditionId) {
        return EstimateMapper.toResponse(estimateExpedition.estimate(new ExpeditionId(expeditionId)));
    }
}
