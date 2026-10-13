package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

public record ValidationResponse(boolean canBeApproved, List<ValidationFindingDto> criticals,
                                 List<ValidationFindingDto> warnings) {
}
