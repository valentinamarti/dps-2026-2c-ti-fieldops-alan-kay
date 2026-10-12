package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

/**
 * A consumable an activity needs: how much of a category. The amount travels as text, for the same reason the
 * stock of a supply does (see {@link QuantityDto}).
 */
public record SupplyRequirementDto(String category, QuantityDto quantity) {
}
