package ar.edu.itba.dps.fieldops.business.models.responses;

import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.estimation.ExpeditionEstimate;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;
import ar.edu.itba.dps.fieldops.business.models.report.ExpeditionReport;
import ar.edu.itba.dps.fieldops.business.models.report.ProgressEntry;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;

import java.util.List;
import java.util.Map;

public record ExpeditionReportView(String expeditionName, ExpeditionStatus status, int scheduledActivities,
                                   int finishedActivities, ExpeditionEstimate estimate,
                                   Map<ResourceCategory, Quantity> actualConsumption, List<Incident> incidents,
                                   List<AcceptedWarningView> acceptedWarnings, List<RecordedEventView> timeline,
                                   List<ProgressEntry> progress) {

    public ExpeditionReportView {
        timeline = List.copyOf(timeline);
        progress = List.copyOf(progress);
    }

    public static ExpeditionReportView of(ExpeditionReport report, List<RecordedEventView> timeline,
                                          List<ProgressEntry> progress) {
        return new ExpeditionReportView(report.expeditionName(), report.status(), report.scheduledActivities(),
                report.finishedActivities(), report.estimate(), report.actualConsumption(), report.incidents(),
                report.acceptedWarnings().stream().map(AcceptedWarningView::of).toList(), timeline, progress);
    }
}
