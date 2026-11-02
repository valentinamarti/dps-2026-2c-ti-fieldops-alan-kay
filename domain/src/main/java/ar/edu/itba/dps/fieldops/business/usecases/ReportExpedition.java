package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.responses.ExpeditionReportView;
import ar.edu.itba.dps.fieldops.business.ports.in.ReportExpeditionPort;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.reports.ExpeditionReporter;

import java.util.Objects;

public class ReportExpedition implements ReportExpeditionPort {

    private final ExpeditionRepository repository;
    private final ExpeditionReporter reporter;

    public ReportExpedition(ExpeditionRepository repository, ExpeditionReporter reporter) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
        this.reporter = Objects.requireNonNull(reporter, "reporter is required");
    }

    @Override
    public ExpeditionReportView report(ExpeditionId id) {
        return ExpeditionReportView.of(reporter.reportOf(Lookups.expedition(repository, id)));
    }
}
