package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

/**
 * A reusable resource an activity needs: how many of a category. The category comes normalized, like the one
 * of the equipment that covers it (D34).
 */
public record EquipmentRequirementDto(String category, int count) {
}
