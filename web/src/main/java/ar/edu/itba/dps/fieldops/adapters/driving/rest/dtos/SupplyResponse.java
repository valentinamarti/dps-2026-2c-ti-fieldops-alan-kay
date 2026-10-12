package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

/**
 * A depletable supply of the catalog as the API shows it. The stock is the one left: it goes down when an
 * expedition consumes it, and what is reserved is not shown because it belongs to each expedition.
 */
public record SupplyResponse(String id, String name, String category, QuantityDto stock) {
}
