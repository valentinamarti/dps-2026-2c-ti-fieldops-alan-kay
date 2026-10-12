package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;

/**
 * Everything needed to register a depletable supply in the catalog. The unit of the expedition demands is the
 * one of its initial stock.
 */
public record RegisterSupplyCommand(ResourceId id, String name, ResourceCategory category, Quantity initialStock) {
}
