package ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos;

import java.util.List;

public record ApprovalRequest(List<WarningAcceptanceRequest> acceptedWarnings) {
}
