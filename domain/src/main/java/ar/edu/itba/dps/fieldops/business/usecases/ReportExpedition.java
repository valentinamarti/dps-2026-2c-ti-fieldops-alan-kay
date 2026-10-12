package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.report.ExpeditionReport;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.reports.ExpeditionReporter;

import java.util.Objects;

public class ReportExpedition {

    private final ExpeditionRepository repository;
    private final ExpeditionReporter reporter;

    public ReportExpedition(ExpeditionRepository repository, ExpeditionReporter reporter) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
        this.reporter = Objects.requireNonNull(reporter, "reporter is required");
    }

    public ExpeditionReport report(ExpeditionId id) {
        return reporter.reportOf(Lookups.expedition(repository, id));
    }
}
