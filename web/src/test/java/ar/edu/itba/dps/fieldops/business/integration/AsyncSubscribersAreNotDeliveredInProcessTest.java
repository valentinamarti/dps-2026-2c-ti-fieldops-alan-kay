package ar.edu.itba.dps.fieldops.business.integration;

import ar.edu.itba.dps.fieldops.business.interfaces.events.AsyncEventSubscriber;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.commands.RegisterIncidentCommand;
import ar.edu.itba.dps.fieldops.business.models.events.ConsumerName;
import ar.edu.itba.dps.fieldops.business.models.events.IncidentRegistered;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentReport;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.ports.in.ApproveExpeditionPort;
import ar.edu.itba.dps.fieldops.business.ports.in.QueryReplanProposalsPort;
import ar.edu.itba.dps.fieldops.business.ports.in.RegisterIncidentPort;
import ar.edu.itba.dps.fieldops.business.ports.in.TransitionExpeditionPort;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.instrument;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.supply;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;
import static ar.edu.itba.dps.fieldops.business.models.common.Quantity.of;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Sql("/clean-database.sql")
class AsyncSubscribersAreNotDeliveredInProcessTest {
    private static final LocalDateTime TEN = LocalDateTime.of(2026, 10, 1, 10, 0);

    @TestConfiguration
    static class WithAnAsyncSubscriber {
        @Bean
        RecordingSubscriber recordingSubscriber() {
            return new RecordingSubscriber();
        }
    }

    static final class RecordingSubscriber implements AsyncEventSubscriber<IncidentRegistered> {
        private final List<IncidentRegistered> received = new ArrayList<>();

        @Override
        public Class<IncidentRegistered> eventType() {
            return IncidentRegistered.class;
        }

        @Override
        public ConsumerName consumerName() {
            return new ConsumerName("recording");
        }

        @Override
        public void on(IncidentRegistered event) {
            received.add(event);
        }
    }

    @Autowired
    private RecordingSubscriber asyncSubscriber;
    @Autowired
    private ExpeditionRepository expeditions;
    @Autowired
    private ApproveExpeditionPort approveExpedition;
    @Autowired
    private TransitionExpeditionPort transitions;
    @Autowired
    private RegisterIncidentPort registerIncident;
    @Autowired
    private QueryReplanProposalsPort queryReplanProposals;
    @Autowired
    private ResourceCatalog catalog;
    @Autowired
    private ZoneRepository zones;

    private final Zone coast = zone("z-1");
    private final Person leader = person("p-1");
    private final Person sampler = person("p-2");
    private final Activity sampling = sampling("a-1", coast, hours(8, 18));

    @Test
    void registeringAnIncidentRunsTheSynchronousSubscriberButNotTheAsynchronousOne() {
        final var expedition = approvedAndRunning();

        registerIncident.register(new RegisterIncidentCommand(expedition, sampling.getId(), IncidentReport.of("Storm", TEN)));

        assertEquals(1, queryReplanProposals.ofExpedition(expedition).size());
        assertEquals(List.of(), asyncSubscriber.received);
    }

    private ExpeditionId approvedAndRunning() {
        zones.add(coast);
        List.of(leader, sampler).forEach(catalog::addStaff);
        final var cooler = instrument("c-1", "Cooler");
        final var jars = supply("j-1", "Sample container", of("10", UNITS));
        catalog.addEquipment(cooler);
        catalog.addSupply(jars);
        final var expedition = expeditionIn(coast, leader).build(expeditions);
        final var item = expedition.schedule(sampling, hours(13, 15));
        expedition.assignStaff(item, sampler);
        expedition.assignEquipment(item, cooler);
        expedition.assignSupply(item, jars);
        expedition.submitForReview();
        expeditions.add(expedition);
        approveExpedition.approve(expedition.getId(), List.of());
        transitions.start(expedition.getId());
        return expedition.getId();
    }
}
