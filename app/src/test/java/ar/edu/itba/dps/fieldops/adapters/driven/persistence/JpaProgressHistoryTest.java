package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.events.EventId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.report.ProgressEntry;
import ar.edu.itba.dps.fieldops.business.providers.ProgressHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class JpaProgressHistoryTest {
    private static final ExpeditionId PATAGONIA = new ExpeditionId("e-1");
    private static final ExpeditionId NIGHT_SURVEY = new ExpeditionId("e-2");
    private static final LocalDateTime TEN = LocalDateTime.of(2026, 2, 1, 10, 0);

    @Autowired
    private ProgressHistory history;
    @Autowired
    private DatabaseCleaner cleaner;

    private static ProgressEntry entry(String event, ExpeditionId expedition, LocalDateTime at, int finished) {
        return new ProgressEntry(new EventId(event), expedition, new ActivityId("a-1"), at, finished, 4);
    }

    @BeforeEach
    void startEmpty() {
        cleaner.clean();
    }

    @Test
    void givesBackWhatItSaved() {
        final var saved = entry("ev-1", PATAGONIA, TEN, 1);

        history.add(saved);

        assertEquals(List.of(saved), history.ofExpedition(PATAGONIA));
    }

    @Test
    void keepsTheOrderTheyArrivedInEvenIfTheFactsHappenedInAnotherOrder() {
        final var later = entry("ev-1", PATAGONIA, TEN.plusHours(1), 1);
        final var earlier = entry("ev-2", PATAGONIA, TEN, 2);

        history.add(later);
        history.add(earlier);

        assertEquals(List.of(later, earlier), history.ofExpedition(PATAGONIA));
    }

    @Test
    void leavesOutTheEntriesOfAnotherExpedition() {
        final var ofPatagonia = entry("ev-1", PATAGONIA, TEN, 1);
        history.add(ofPatagonia);
        history.add(entry("ev-2", NIGHT_SURVEY, TEN, 1));

        assertEquals(List.of(ofPatagonia), history.ofExpedition(PATAGONIA));
    }

    @Test
    void rejectsASecondEntryOfTheSameEvent() {
        history.add(entry("ev-1", PATAGONIA, TEN, 1));

        assertThrows(DataIntegrityViolationException.class, () -> history.add(entry("ev-1", PATAGONIA, TEN, 1)));
        assertEquals(1, history.ofExpedition(PATAGONIA).size());
    }
}
