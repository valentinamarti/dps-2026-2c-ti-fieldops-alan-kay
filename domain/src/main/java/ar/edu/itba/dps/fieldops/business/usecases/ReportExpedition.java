package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.responses.ExpeditionReportView;
import ar.edu.itba.dps.fieldops.business.ports.in.ReportExpeditionPort;
import ar.edu.itba.dps.fieldops.business.providers.EventLog;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ProgressHistory;
import ar.edu.itba.dps.fieldops.business.reports.ExpeditionReporter;

import java.util.Objects;

public class ReportExpedition implements ReportExpeditionPort {

    private final ExpeditionRepository repository;
    private final ExpeditionReporter reporter;
    private final EventLog events;
    private final ProgressHistory progress;

    public ReportExpedition(ExpeditionRepository repository, ExpeditionReporter reporter, EventLog events,
                            ProgressHistory progress) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
        this.reporter = Objects.requireNonNull(reporter, "reporter is required");
        this.events = Objects.requireNonNull(events, "events are required");
        this.progress = Objects.requireNonNull(progress, "progress is required");
    }

    @Override
    public ExpeditionReportView report(ExpeditionId id) {
        final var expedition = Lookups.expedition(repository, id);
        return ExpeditionReportView.of(reporter.reportOf(expedition), events.ofExpedition(expedition.getId()),
                progress.ofExpedition(expedition.getId()));
    }
}
