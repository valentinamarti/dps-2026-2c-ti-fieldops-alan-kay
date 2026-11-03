package ar.edu.itba.dps.fieldops.business.models.responses;

import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.resources.Depletable;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;

public record SupplyView(ResourceId id, String name, ResourceCategory category, Quantity stock) {

    public static SupplyView of(Depletable supply) {
        return new SupplyView(supply.getId(), supply.getName(), supply.getCategory(), supply.getStock());
    }
}
