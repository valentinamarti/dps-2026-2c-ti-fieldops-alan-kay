package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

/**
 * What the approval rules have to say about an expedition, without approving it (D22, D32). Criticals and
 * warnings travel apart because they are not the same thing for whoever reads them: a critical has to be fixed
 * and a warning can be accepted with a justification, which is what the body of the approval carries.
 *
 * @param canBeApproved whether there is no critical left, which is the only condition the approval checks
 */
public record ValidationResponse(boolean canBeApproved, List<ValidationFindingDto> criticals,
                                 List<ValidationFindingDto> warnings) {
}
