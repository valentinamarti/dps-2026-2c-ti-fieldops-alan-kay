package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.RegisterSupplyRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.SupplyResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.SupplyMapper;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.usecases.QueryCatalog;
import ar.edu.itba.dps.fieldops.business.usecases.RegisterResources;
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

import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * The depletable supplies of the catalog. There is no unavailabilities sub-resource here: a supply has stock,
 * not a calendar (D52).
 */
@RestController
@RequestMapping("/api/supplies")
@Tag(name = "Supplies", description = "The depletable supplies of the catalog")
public class SupplyController {

    private final RegisterResources registerResources;
    private final QueryCatalog queryCatalog;

    public SupplyController(RegisterResources registerResources, QueryCatalog queryCatalog) {
        this.registerResources = Objects.requireNonNull(registerResources, "registerResources is required");
        this.queryCatalog = Objects.requireNonNull(queryCatalog, "queryCatalog is required");
    }

    @PostMapping
    @Operation(summary = "Registers a depletable supply in the catalog")
    @ApiResponse(responseCode = "201", description = "Registered, with its location in the Location header")
    @ApiResponse(responseCode = "400", description = "A field is missing or does not make sense")
    @ApiResponse(responseCode = "409", description = "The id is already taken by another resource")
    public ResponseEntity<SupplyResponse> register(@RequestBody RegisterSupplyRequest request) {
        final var registered = SupplyMapper.toResponse(registerResources.registerSupply(SupplyMapper.toCommand(request)));
        return ResponseEntity.created(URI.create("/api/supplies/" + registered.id())).body(registered);
    }

    @GetMapping
    @Operation(summary = "Lists the supplies, in the order they were registered")
    @ApiResponse(responseCode = "200", description = "The supplies of the catalog, with the stock they have left")
    public List<SupplyResponse> list() {
        return queryCatalog.supplies().stream().map(SupplyMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets one supply by id")
    @ApiResponse(responseCode = "200", description = "The supply, with the stock it has left")
    @ApiResponse(responseCode = "404", description = "There is no supply with that id")
    public SupplyResponse get(@PathVariable String id) {
        return SupplyMapper.toResponse(queryCatalog.supply(new ResourceId(id)));
    }
}
