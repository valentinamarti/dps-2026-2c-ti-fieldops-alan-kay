package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.estimations.ExpeditionEstimator;
import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.events.EventId;
import ar.edu.itba.dps.fieldops.business.models.report.ProgressEntry;
import ar.edu.itba.dps.fieldops.business.models.responses.ExpeditionReportView;
import ar.edu.itba.dps.fieldops.business.models.responses.RecordedEventView;
import ar.edu.itba.dps.fieldops.business.providers.EventLog;
import ar.edu.itba.dps.fieldops.business.providers.ProgressHistory;
import ar.edu.itba.dps.fieldops.business.reports.ExpeditionReporter;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportExpeditionTest {
    private static final LocalDateTime TEN = LocalDateTime.of(2026, 10, 1, 10, 0);

    @Test
    void addsTheTimelineAndTheProgressToWhatTheReporterCalculates() {
        final var expeditions = new SequentialExpeditionRepository();
        final var reporter = new ExpeditionReporter(new ExpeditionEstimator());
        final var expedition = expeditionIn(zone("z-1"), person("p-1")).build(expeditions);
        expeditions.add(expedition);
        final var timeline = List.of(new RecordedEventView(new EventId("ev-1"), "ActivityCompleted", TEN, TEN, List.of()));
        final var progress = List.of(new ProgressEntry(new EventId("ev-1"), expedition.getId(), new ActivityId("a-1"), TEN, 1, 2));
        final EventLog events = mock(EventLog.class);
        final ProgressHistory history = mock(ProgressHistory.class);
        when(events.ofExpedition(expedition.getId())).thenReturn(timeline);
        when(history.ofExpedition(expedition.getId())).thenReturn(progress);

        final var report = new ReportExpedition(expeditions, reporter, events, history).report(expedition.getId());

        assertEquals(ExpeditionReportView.of(reporter.reportOf(expedition), timeline, progress), report);
    }
}
