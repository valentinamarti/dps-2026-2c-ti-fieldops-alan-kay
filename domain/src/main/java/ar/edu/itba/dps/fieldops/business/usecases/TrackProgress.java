package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.interfaces.events.AsyncEventSubscriber;
import ar.edu.itba.dps.fieldops.business.models.events.ActivityCompleted;
import ar.edu.itba.dps.fieldops.business.models.events.ConsumerName;
import ar.edu.itba.dps.fieldops.business.models.report.ProgressEntry;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ProgressHistory;
import ar.edu.itba.dps.fieldops.business.reports.ExpeditionReporter;

import java.util.Objects;

public class TrackProgress implements AsyncEventSubscriber<ActivityCompleted> {
    private static final ConsumerName NAME = new ConsumerName("track-progress");

    private final ExpeditionRepository expeditions;
    private final ExpeditionReporter reporter;
    private final ProgressHistory history;

    public TrackProgress(ExpeditionRepository expeditions, ExpeditionReporter reporter, ProgressHistory history) {
        this.expeditions = Objects.requireNonNull(expeditions, "expeditions are required");
        this.reporter = Objects.requireNonNull(reporter, "reporter is required");
        this.history = Objects.requireNonNull(history, "history is required");
    }

    @Override
    public Class<ActivityCompleted> eventType() {
        return ActivityCompleted.class;
    }

    @Override
    public ConsumerName consumerName() {
        return NAME;
    }

    @Override
    public void on(ActivityCompleted event) {
        final var report = reporter.reportOf(Lookups.expedition(expeditions, event.expeditionId()));
        history.add(ProgressEntry.of(event, report));
    }
}
