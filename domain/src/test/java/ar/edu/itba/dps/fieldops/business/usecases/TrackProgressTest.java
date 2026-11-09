package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.estimations.ExpeditionEstimator;
import ar.edu.itba.dps.fieldops.business.exceptions.NotFoundException;
import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.events.ActivityCompleted;
import ar.edu.itba.dps.fieldops.business.models.events.ConsumerName;
import ar.edu.itba.dps.fieldops.business.models.events.EventId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.report.ProgressEntry;
import ar.edu.itba.dps.fieldops.business.providers.ProgressHistory;
import ar.edu.itba.dps.fieldops.business.reports.ExpeditionReporter;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrackProgressTest {
    private static final EventId EVENT = new EventId("ev-1");
    private static final ActivityId ACTIVITY = new ActivityId("a-1");
    private static final LocalDateTime TEN = LocalDateTime.of(2026, 10, 1, 10, 0);

    private static final class RecordingHistory implements ProgressHistory {
        private final List<ProgressEntry> added = new ArrayList<>();

        @Override
        public void add(ProgressEntry entry) {
            added.add(entry);
        }

        @Override
        public List<ProgressEntry> ofExpedition(ExpeditionId expedition) {
            return added.stream().filter(entry -> entry.expedition().equals(expedition)).toList();
        }
    }

    private final SequentialExpeditionRepository expeditions = new SequentialExpeditionRepository();
    private final RecordingHistory history = new RecordingHistory();
    private final TrackProgress consumer =
            new TrackProgress(expeditions, new ExpeditionReporter(new ExpeditionEstimator()), history);

    private static ActivityCompleted completedOn(ExpeditionId expedition) {
        return new ActivityCompleted(EVENT, expedition, ACTIVITY, TEN, "Twelve samples collected");
    }

    @Test
    void writesDownTheProgressTheReportCalculates() {
        final var coast = zone("z-1");
        final var expedition = expeditionIn(coast, person("p-1")).build(expeditions);
        expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(9, 11));
        expedition.schedule(sampling("a-2", coast, hours(0, 24)), hours(14, 16));
        expeditions.add(expedition);

        consumer.on(completedOn(expedition.getId()));

        assertEquals(List.of(new ProgressEntry(EVENT, expedition.getId(), ACTIVITY, TEN, 0, 2)), history.added);
    }

    @Test
    void anExpeditionThatDoesNotExistFailsAndWritesNothing() {
        final var event = completedOn(new ExpeditionId("e-9"));

        assertThrows(NotFoundException.class, () -> consumer.on(event));

        assertEquals(List.of(), history.added);
    }

    @Test
    void asksOnlyForCompletedActivitiesUnderAStableName() {
        assertEquals(ActivityCompleted.class, consumer.eventType());
        assertEquals(new ConsumerName("track-progress"), consumer.consumerName());
    }
}
