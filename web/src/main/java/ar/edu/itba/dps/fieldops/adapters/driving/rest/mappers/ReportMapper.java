package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.AcceptedWarningDto;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ExpeditionReportResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ProgressEntryResponse;
import ar.edu.itba.dps.fieldops.business.models.report.ProgressEntry;
import ar.edu.itba.dps.fieldops.business.models.responses.AcceptedWarningView;
import ar.edu.itba.dps.fieldops.business.models.responses.ExpeditionReportView;

public final class ReportMapper {
    private ReportMapper() {
    }

    public static ExpeditionReportResponse toResponse(ExpeditionReportView report) {
        return new ExpeditionReportResponse(
                report.expeditionName(),
                report.status(),
                report.scheduledActivities(),
                report.finishedActivities(),
                EstimateMapper.toResponse(report.estimate()),
                EstimateMapper.toConsumptionDtos(report.actualConsumption()),
                report.incidents().stream().map(ReplanningMapper::toDto).toList(),
                report.acceptedWarnings().stream().map(ReportMapper::toDto).toList(),
                report.timeline().stream().map(EventLogMapper::toResponse).toList(),
                report.progress().stream().map(ReportMapper::toResponse).toList());
    }

    private static ProgressEntryResponse toResponse(ProgressEntry entry) {
        return new ProgressEntryResponse(entry.activity().value(), entry.occurredAt(), entry.finishedActivities(),
                entry.scheduledActivities());
    }

    private static AcceptedWarningDto toDto(AcceptedWarningView warning) {
        return new AcceptedWarningDto(
                ValidationMapper.toDto(warning.warning()),
                warning.responsibleId().value(),
                warning.justification());
    }
}
