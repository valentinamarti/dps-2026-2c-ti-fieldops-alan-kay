package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

public record EquipmentCatalogResponse(List<VehicleResponse> vehicles, List<InstrumentResponse> instruments) {
}
