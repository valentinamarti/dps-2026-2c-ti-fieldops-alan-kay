package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.InstrumentResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.RegisterInstrumentRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.UnavailabilityDto;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.InstrumentMapper;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.UnavailabilityMapper;
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

@RestController
@RequestMapping("/api/instruments")
@Tag(name = "Instruments", description = "The instruments of the catalog and their unavailabilities")
public class InstrumentController {
    private final RegisterResources registerResources;
    private final QueryCatalog queryCatalog;
    private final MarkResourceUnavailable markResourceUnavailable;

    public InstrumentController(RegisterResources registerResources, QueryCatalog queryCatalog,
                               MarkResourceUnavailable markResourceUnavailable) {
        this.registerResources = Objects.requireNonNull(registerResources, "registerResources is required");
        this.queryCatalog = Objects.requireNonNull(queryCatalog, "queryCatalog is required");
        this.markResourceUnavailable = Objects.requireNonNull(markResourceUnavailable, "markResourceUnavailable is required");
    }

    @PostMapping
    @Operation(summary = "Registers an instrument in the catalog")
    @ApiResponse(responseCode = "201", description = "Registered, with its location in the Location header")
    @ApiResponse(responseCode = "400", description = "A field is missing or does not make sense")
    @ApiResponse(responseCode = "409", description = "The id is already taken by another resource")
    public ResponseEntity<InstrumentResponse> register(@RequestBody RegisterInstrumentRequest request) {
        final var registered = InstrumentMapper.toResponse(
                registerResources.registerInstrument(InstrumentMapper.toCommand(request)));
        return ResponseEntity.created(URI.create("/api/instruments/" + registered.id())).body(registered);
    }

    @GetMapping
    @Operation(summary = "Lists the instruments, in the order they were registered")
    @ApiResponse(responseCode = "200", description = "The instruments of the catalog, without the vehicles")
    public List<InstrumentResponse> list() {
        return queryCatalog.instruments().stream().map(InstrumentMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets one instrument by id")
    @ApiResponse(responseCode = "200", description = "The instrument")
    @ApiResponse(responseCode = "404", description = "There is no instrument with that id")
    public InstrumentResponse get(@PathVariable String id) {
        return InstrumentMapper.toResponse(queryCatalog.instrument(new ResourceId(id)));
    }

    @PostMapping("/{id}/unavailabilities")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Records a period in which the instrument cannot be used")
    @ApiResponse(responseCode = "204", description = "Recorded; the unavailability is not a resource that can be read back")
    @ApiResponse(responseCode = "400", description = "The period or the reason is missing or does not make sense")
    @ApiResponse(responseCode = "404", description = "There is no instrument with that id")
    public void markUnavailable(@PathVariable String id, @RequestBody UnavailabilityDto request) {
        markResourceUnavailable.markInstrumentUnavailable(new ResourceId(id), UnavailabilityMapper.toUnavailability(request));
    }
}
