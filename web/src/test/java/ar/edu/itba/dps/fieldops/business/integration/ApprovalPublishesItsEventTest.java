package ar.edu.itba.dps.fieldops.business.integration;

import ar.edu.itba.dps.fieldops.business.interfaces.events.EventSubscriber;
import ar.edu.itba.dps.fieldops.business.models.events.ExpeditionApproved;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/clean-database.sql")
class ApprovalPublishesItsEventTest {

    @TestConfiguration
    static class WithAnApprovalSubscriber {
        @Bean
        RecordingSubscriber recordingSubscriber() {
            return new RecordingSubscriber();
        }
    }

    static final class RecordingSubscriber implements EventSubscriber<ExpeditionApproved> {
        private final List<ExpeditionApproved> received = new ArrayList<>();

        @Override
        public Class<ExpeditionApproved> eventType() {
            return ExpeditionApproved.class;
        }

        @Override
        public void on(ExpeditionApproved event) {
            received.add(event);
        }
    }

    @Autowired
    private MockMvc api;
    @Autowired
    private RecordingSubscriber subscriber;
    @Autowired
    private ExpeditionRepository expeditions;
    @Autowired
    private ResourceCatalog catalog;
    @Autowired
    private ZoneRepository zones;

    private final Zone coast = zone("z-1");
    private final Person leader = person("p-1");

    @BeforeEach
    void forgetEventsFromPreviousTests() {
        subscriber.received.clear();
    }

    @Test
    void approvingThroughTheApiPublishesExpeditionApprovedAtTheTimeTheServerReceivedTheRequest() throws Exception {
        final var expedition = inReview();
        final var before = LocalDateTime.now();

        api.perform(post("/api/expeditions/{id}/approval", expedition.value())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"acceptedWarnings\": []}"))
                .andExpect(status().isNoContent());

        final var after = LocalDateTime.now();
        assertEquals(1, subscriber.received.size());
        final var event = subscriber.received.getFirst();
        assertEquals(expedition, event.expeditionId());
        assertFalse(event.occurredAt().isBefore(before));
        assertFalse(event.occurredAt().isAfter(after));
    }

    private ExpeditionId inReview() {
        zones.add(coast);
        catalog.addStaff(leader);
        final var expedition = expeditionIn(coast, leader).build(expeditions);
        expedition.submitForReview();
        expeditions.add(expedition);
        return expedition.getId();
    }
}
