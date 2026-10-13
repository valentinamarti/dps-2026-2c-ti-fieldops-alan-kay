package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

public record RegisterSupplyRequest(String id, String name, String category, QuantityDto initialStock) {
}
