package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.AcceptedWarningDto;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ExpeditionReportResponse;
import ar.edu.itba.dps.fieldops.business.models.report.ExpeditionReport;
import ar.edu.itba.dps.fieldops.business.models.validation.AcceptedWarning;

/**
 * Turns the report of an expedition into its answer (D49, D57). The plan side of it reuses
 * {@link EstimateMapper}: {@code estimate} is the same {@code ExpeditionEstimate} that mapper already
 * translates, and {@code actualConsumption} is the same category-and-quantity shape, so the two are read the
 * same way even though one is a plan and the other is what happened. The warnings side reuses
 * {@link ValidationMapper}'s translation of a finding, closing the loop a {@code WarningAcceptanceRequest}
 * opened by copying one out of the validation. The incidents side reuses {@link ReplanningMapper}, which owns
 * the concept (D48, D58): the log of the expedition and the incident of a proposal are the same incident.
 */
public final class ReportMapper {

    private ReportMapper() {
    }

    public static ExpeditionReportResponse toResponse(ExpeditionReport report) {
        return new ExpeditionReportResponse(
                report.expeditionName(),
                report.status(),
                report.scheduledActivities(),
                report.finishedActivities(),
                EstimateMapper.toResponse(report.estimate()),
                EstimateMapper.toConsumptionDtos(report.actualConsumption()),
                report.incidents().stream().map(ReplanningMapper::toDto).toList(),
                report.acceptedWarnings().stream().map(ReportMapper::toDto).toList());
    }

    private static AcceptedWarningDto toDto(AcceptedWarning warning) {
        return new AcceptedWarningDto(
                ValidationMapper.toDto(warning.warning()),
                warning.responsible().getId().value(),
                warning.justification());
    }
}
