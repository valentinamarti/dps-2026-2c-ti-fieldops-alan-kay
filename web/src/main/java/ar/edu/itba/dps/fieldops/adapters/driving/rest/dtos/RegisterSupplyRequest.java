package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

/**
 * The body of {@code POST /api/supplies}. The unit of the initial stock is the one every demand of this supply
 * is expressed in, so a stock in liters cannot be asked for in units.
 */
public record RegisterSupplyRequest(String id, String name, String category, QuantityDto initialStock) {
}
