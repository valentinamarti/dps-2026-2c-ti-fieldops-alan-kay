package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.CreateExpeditionRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ExpeditionResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.ExpeditionMapper;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.usecases.CreateExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.QueryExpeditions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * The expedition resource. It only translates: the request becomes a command, the use case does the work and
 * the result becomes a DTO (D49). Nothing here knows the rules.
 */
@RestController
@RequestMapping("/api/expeditions")
@Tag(name = "Expeditions", description = "Creating and querying expeditions")
public class ExpeditionController {

    private final CreateExpedition createExpedition;
    private final QueryExpeditions queryExpeditions;

    public ExpeditionController(CreateExpedition createExpedition, QueryExpeditions queryExpeditions) {
        this.createExpedition = Objects.requireNonNull(createExpedition, "createExpedition is required");
        this.queryExpeditions = Objects.requireNonNull(queryExpeditions, "queryExpeditions is required");
    }

    @PostMapping
    @Operation(summary = "Creates an expedition in draft")
    @ApiResponse(responseCode = "201", description = "Created, with its location in the Location header")
    @ApiResponse(responseCode = "400", description = "A field is missing or does not make sense")
    @ApiResponse(responseCode = "404", description = "A zone or a responsible does not exist")
    public ResponseEntity<ExpeditionResponse> create(@RequestBody CreateExpeditionRequest request) {
        final var created = ExpeditionMapper.toResponse(createExpedition.create(ExpeditionMapper.toCommand(request)));
        return ResponseEntity.created(Locations.expedition(created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "Lists every expedition, in the order they were created")
    @ApiResponse(responseCode = "200", description = "The expeditions, without their itineraries")
    public List<ExpeditionResponse> list() {
        return queryExpeditions.list().stream().map(ExpeditionMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets one expedition by id")
    @ApiResponse(responseCode = "200", description = "The expedition, without its itinerary")
    @ApiResponse(responseCode = "404", description = "There is no expedition with that id")
    public ExpeditionResponse get(@PathVariable String id) {
        return ExpeditionMapper.toResponse(queryExpeditions.get(new ExpeditionId(id)));
    }
}
