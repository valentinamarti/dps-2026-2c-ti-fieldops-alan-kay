package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.NotFoundException;
import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.models.events.EventId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.responses.RecordedEventView;
import ar.edu.itba.dps.fieldops.business.providers.EventLog;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryExpeditionEventsTest {
    private static final LocalDateTime TEN = LocalDateTime.of(2026, 10, 1, 10, 0);
    private static final RecordedEventView APPROVED =
            new RecordedEventView(new EventId("ev-1"), "ExpeditionApproved", TEN, TEN, List.of());

    private final List<ExpeditionId> asked = new ArrayList<>();
    private final EventLog log = expedition -> {
        asked.add(expedition);
        return List.of(APPROVED);
    };
    private final SequentialExpeditionRepository expeditions = new SequentialExpeditionRepository();
    private final QueryExpeditionEvents query = new QueryExpeditionEvents(log, expeditions);

    @Test
    void handsOutWhatTheLogHasForTheExpedition() {
        final var expedition = expeditionIn(zone("z-1"), person("p-1")).build(expeditions);
        expeditions.add(expedition);

        assertEquals(List.of(APPROVED), query.ofExpedition(expedition.getId()));
        assertEquals(List.of(expedition.getId()), asked);
    }

    @Test
    void anExpeditionThatDoesNotExistIsNotFoundAndTheLogIsNotAsked() {
        assertThrows(NotFoundException.class, () -> query.ofExpedition(new ExpeditionId("e-9")));

        assertEquals(List.of(), asked);
    }
}
