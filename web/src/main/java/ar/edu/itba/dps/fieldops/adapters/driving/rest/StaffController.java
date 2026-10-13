package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.RegisterStaffRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.StaffResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.UnavailabilityDto;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.StaffMapper;
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
@RequestMapping("/api/staff")
@Tag(name = "Staff", description = "The people of the catalog and their unavailabilities")
public class StaffController {
    private final RegisterResources registerResources;
    private final QueryCatalog queryCatalog;
    private final MarkResourceUnavailable markResourceUnavailable;

    public StaffController(RegisterResources registerResources, QueryCatalog queryCatalog,
                           MarkResourceUnavailable markResourceUnavailable) {
        this.registerResources = Objects.requireNonNull(registerResources, "registerResources is required");
        this.queryCatalog = Objects.requireNonNull(queryCatalog, "queryCatalog is required");
        this.markResourceUnavailable = Objects.requireNonNull(markResourceUnavailable, "markResourceUnavailable is required");
    }

    @PostMapping
    @Operation(summary = "Registers a person in the catalog")
    @ApiResponse(responseCode = "201", description = "Registered, with its location in the Location header")
    @ApiResponse(responseCode = "400", description = "A field is missing or does not make sense")
    @ApiResponse(responseCode = "409", description = "The id is already taken by another resource")
    public ResponseEntity<StaffResponse> register(@RequestBody RegisterStaffRequest request) {
        final var registered = StaffMapper.toResponse(registerResources.registerPerson(StaffMapper.toCommand(request)));
        return ResponseEntity.created(URI.create("/api/staff/" + registered.id())).body(registered);
    }

    @GetMapping
    @Operation(summary = "Lists the staff, in the order they were registered")
    @ApiResponse(responseCode = "200", description = "The people of the catalog")
    public List<StaffResponse> list() {
        return queryCatalog.staff().stream().map(StaffMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets one person by id")
    @ApiResponse(responseCode = "200", description = "The person")
    @ApiResponse(responseCode = "404", description = "There is no person with that id")
    public StaffResponse get(@PathVariable String id) {
        return StaffMapper.toResponse(queryCatalog.person(new ResourceId(id)));
    }

    @PostMapping("/{id}/unavailabilities")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Records a period in which the person cannot be used")
    @ApiResponse(responseCode = "204", description = "Recorded; the unavailability is not a resource that can be read back")
    @ApiResponse(responseCode = "400", description = "The period or the reason is missing or does not make sense")
    @ApiResponse(responseCode = "404", description = "There is no person with that id")
    public void markUnavailable(@PathVariable String id, @RequestBody UnavailabilityDto request) {
        markResourceUnavailable.markStaffUnavailable(new ResourceId(id), UnavailabilityMapper.toUnavailability(request));
    }
}
