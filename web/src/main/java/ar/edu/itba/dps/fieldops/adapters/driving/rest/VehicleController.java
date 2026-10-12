package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.RegisterVehicleRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.UnavailabilityDto;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.VehicleResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.UnavailabilityMapper;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.VehicleMapper;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.usecases.MarkResourceUnavailable;
import ar.edu.itba.dps.fieldops.business.usecases.QueryCatalog;
import ar.edu.itba.dps.fieldops.business.usecases.RegisterResources;
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

import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * The vehicles of the catalog: the equipment that has a license plate. The instruments are the other kind of
 * equipment and have their own resource; {@code GET /api/equipment} lists both (D52).
 */
@RestController
@RequestMapping("/api/vehicles")
@Tag(name = "Vehicles", description = "The vehicles of the catalog and their unavailabilities")
public class VehicleController {

    private final RegisterResources registerResources;
    private final QueryCatalog queryCatalog;
    private final MarkResourceUnavailable markResourceUnavailable;

    public VehicleController(RegisterResources registerResources, QueryCatalog queryCatalog,
                             MarkResourceUnavailable markResourceUnavailable) {
        this.registerResources = Objects.requireNonNull(registerResources, "registerResources is required");
        this.queryCatalog = Objects.requireNonNull(queryCatalog, "queryCatalog is required");
        this.markResourceUnavailable = Objects.requireNonNull(markResourceUnavailable, "markResourceUnavailable is required");
    }

    @PostMapping
    @Operation(summary = "Registers a vehicle in the catalog")
    @ApiResponse(responseCode = "201", description = "Registered, with its location in the Location header")
    @ApiResponse(responseCode = "400", description = "A field is missing or does not make sense")
    @ApiResponse(responseCode = "409", description = "The id is already taken by another resource")
    public ResponseEntity<VehicleResponse> register(@RequestBody RegisterVehicleRequest request) {
        final var registered = VehicleMapper.toResponse(registerResources.registerVehicle(VehicleMapper.toCommand(request)));
        return ResponseEntity.created(URI.create("/api/vehicles/" + registered.id())).body(registered);
    }

    @GetMapping
    @Operation(summary = "Lists the vehicles, in the order they were registered")
    @ApiResponse(responseCode = "200", description = "The vehicles of the catalog, without the instruments")
    public List<VehicleResponse> list() {
        return queryCatalog.vehicles().stream().map(VehicleMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets one vehicle by id")
    @ApiResponse(responseCode = "200", description = "The vehicle")
    @ApiResponse(responseCode = "404", description = "There is no vehicle with that id")
    public VehicleResponse get(@PathVariable String id) {
        return VehicleMapper.toResponse(queryCatalog.vehicle(new ResourceId(id)));
    }

    @PostMapping("/{id}/unavailabilities")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Records a period in which the vehicle cannot be used")
    @ApiResponse(responseCode = "204", description = "Recorded; the unavailability is not a resource that can be read back")
    @ApiResponse(responseCode = "400", description = "The period or the reason is missing or does not make sense")
    @ApiResponse(responseCode = "404", description = "There is no vehicle with that id")
    public void markUnavailable(@PathVariable String id, @RequestBody UnavailabilityDto request) {
        markResourceUnavailable.markVehicleUnavailable(new ResourceId(id), UnavailabilityMapper.toUnavailability(request));
    }
}
