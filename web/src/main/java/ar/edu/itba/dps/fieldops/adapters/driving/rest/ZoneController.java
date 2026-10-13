package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.RegisterZoneRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ZoneResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.ZoneMapper;
import ar.edu.itba.dps.fieldops.business.usecases.QueryZones;
import ar.edu.itba.dps.fieldops.business.usecases.RegisterZone;
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

@RestController
@RequestMapping("/api/zones")
@Tag(name = "Zones", description = "Registering and querying the zones")
public class ZoneController {
    private final RegisterZone registerZone;
    private final QueryZones queryZones;

    public ZoneController(RegisterZone registerZone, QueryZones queryZones) {
        this.registerZone = Objects.requireNonNull(registerZone, "registerZone is required");
        this.queryZones = Objects.requireNonNull(queryZones, "queryZones is required");
    }

    @PostMapping
    @Operation(summary = "Registers a zone")
    @ApiResponse(responseCode = "201", description = "Registered, with its location in the Location header")
    @ApiResponse(responseCode = "400", description = "A field is missing or does not make sense")
    @ApiResponse(responseCode = "409", description = "There is already a zone with that id")
    public ResponseEntity<ZoneResponse> register(@RequestBody RegisterZoneRequest request) {
        final var registered = ZoneMapper.toResponse(registerZone.register(ZoneMapper.toCommand(request)));
        return ResponseEntity.created(URI.create("/api/zones/" + registered.id())).body(registered);
    }

    @GetMapping
    @Operation(summary = "Lists every zone, in the order they were registered")
    @ApiResponse(responseCode = "200", description = "The zones")
    public List<ZoneResponse> list() {
        return queryZones.list().stream().map(ZoneMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets one zone by id")
    @ApiResponse(responseCode = "200", description = "The zone")
    @ApiResponse(responseCode = "404", description = "There is no zone with that id")
    public ZoneResponse get(@PathVariable String id) {
        return ZoneMapper.toResponse(queryZones.get(id));
    }
}
