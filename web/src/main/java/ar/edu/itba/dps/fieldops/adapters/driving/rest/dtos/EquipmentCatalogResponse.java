package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

/**
 * Every piece of equipment of the catalog, grouped by kind. It is a document with two lists and not a flat
 * array because a vehicle and an instrument do not have the same shape: a single array would need a license
 * plate that is sometimes there and sometimes null, which is what having two sub-resources avoids (D52).
 */
public record EquipmentCatalogResponse(List<VehicleResponse> vehicles, List<InstrumentResponse> instruments) {
}
