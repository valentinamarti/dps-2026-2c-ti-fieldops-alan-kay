package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.RegisterSupplyRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.SupplyResponse;
import ar.edu.itba.dps.fieldops.business.models.resources.Depletable;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.usecases.RegisterSupplyCommand;

public final class SupplyMapper {
    private SupplyMapper() {
    }

    public static RegisterSupplyCommand toCommand(RegisterSupplyRequest request) {
        return new RegisterSupplyCommand(
                new ResourceId(request.id()),
                request.name(),
                new ResourceCategory(request.category()),
                Mappings.toQuantity(request.initialStock()));
    }

    public static SupplyResponse toResponse(Depletable supply) {
        return new SupplyResponse(
                supply.getId().value(),
                supply.getName(),
                supply.getCategory().name(),
                Mappings.toDto(supply.getStock()));
    }
}
