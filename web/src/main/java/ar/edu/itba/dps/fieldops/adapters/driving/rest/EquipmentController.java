package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.EquipmentCatalogResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.InstrumentMapper;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers.VehicleMapper;
import ar.edu.itba.dps.fieldops.business.ports.in.QueryCatalogPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/equipment")
@Tag(name = "Equipment", description = "The vehicles and the instruments of the catalog, together")
public class EquipmentController {
    private final QueryCatalogPort queryCatalog;

    public EquipmentController(QueryCatalogPort queryCatalog) {
        this.queryCatalog = Objects.requireNonNull(queryCatalog, "queryCatalog is required");
    }

    @GetMapping
    @Operation(summary = "Lists every piece of equipment, grouped by kind")
    @ApiResponse(responseCode = "200", description = "The vehicles and the instruments, each in its own list")
    public EquipmentCatalogResponse list() {
        return new EquipmentCatalogResponse(
                queryCatalog.vehicles().stream().map(VehicleMapper::toResponse).toList(),
                queryCatalog.instruments().stream().map(InstrumentMapper::toResponse).toList());
    }
}
