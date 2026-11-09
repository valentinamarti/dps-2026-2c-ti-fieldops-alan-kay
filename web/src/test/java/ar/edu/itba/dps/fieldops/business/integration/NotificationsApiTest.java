package ar.edu.itba.dps.fieldops.business.integration;

import ar.edu.itba.dps.fieldops.adapters.driven.EventDispatcher;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.EventDeliveryEntity;
import ar.edu.itba.dps.fieldops.adapters.driven.persistence.EventDeliveryJpaRepository;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.commands.RegisterIncidentCommand;
import ar.edu.itba.dps.fieldops.business.models.events.DeliveryStatus;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentReport;
import ar.edu.itba.dps.fieldops.business.ports.in.RegisterIncidentPort;
import ar.edu.itba.dps.fieldops.business.ports.in.TrackActivityPort;
import ar.edu.itba.dps.fieldops.business.ports.in.TransitionExpeditionPort;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(DispatcherTestConfiguration.class)
@Sql("/clean-database.sql")
class NotificationsApiTest {
    private static final ActivityId ACTIVITY = new ActivityId("a-1");
    private static final LocalDateTime THIRTEEN = LocalDateTime.of(2026, 10, 1, 13, 0);
    private static final String NOTIFY_RESPONSIBLES = "notify-responsibles";

    @Autowired
    private MockMvc api;
    @Autowired
    private ApprovedExpeditions approvedExpeditions;
    @Autowired
    private EventDispatcher dispatcher;
    @Autowired
    private EventDeliveryJpaRepository deliveries;
    @Autowired
    private TransitionExpeditionPort transitions;
    @Autowired
    private TrackActivityPort tracking;
    @Autowired
    private RegisterIncidentPort registerIncident;

    private ResultActions notificationsOf(ExpeditionId expedition) throws Exception {
        return api.perform(get("/api/expeditions/{id}/notifications", expedition.value()));
    }

    @Test
    void theResponsibleIsToldOnceTheDispatcherDeliversTheEvent() throws Exception {
        final var expedition = approvedExpeditions.approveOne();

        dispatcher.dispatchPending();

        notificationsOf(expedition)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventId").value(Matchers.not(Matchers.emptyOrNullString())))
                .andExpect(jsonPath("$[0].responsibleId").value("p-1"))
                .andExpect(jsonPath("$[0].text").value("The expedition was approved"))
                .andExpect(jsonPath("$[0].occurredAt").value("2026-09-30T18:00:00"));
    }

    @Test
    void nobodyIsToldBeforeTheDispatcherRuns() throws Exception {
        final var expedition = approvedExpeditions.approveOne();

        notificationsOf(expedition)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void dispatchingAgainDoesNotNotifyTwice() throws Exception {
        final var expedition = approvedExpeditions.approveOne();

        dispatcher.dispatchPending();
        dispatcher.dispatchPending();

        notificationsOf(expedition).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void theResponsibleIsToldOfEveryFactInTheOrderTheSystemLearnedThem() throws Exception {
        final var expedition = approvedExpeditions.approveOne();
        transitions.start(expedition);
        tracking.start(expedition, ACTIVITY, THIRTEEN);
        registerIncident.register(new RegisterIncidentCommand(expedition, ACTIVITY,
                IncidentReport.of("A storm flooded the station", THIRTEEN.plusHours(2))));
        tracking.finish(expedition, ACTIVITY, THIRTEEN.plusHours(1), "Twelve samples collected");

        dispatcher.dispatchPending();

        notificationsOf(expedition).andExpect(jsonPath("$[*].text").value(Matchers.contains(
                "The expedition was approved",
                "Incident on activity a-1: A storm flooded the station",
                "Activity a-1 finished: Twelve samples collected")));
    }

    @Test
    void anEventDeliveredAgainDoesNotNotifyTwiceAndTheFailureIsVisible() throws Exception {
        final var expedition = approvedExpeditions.approveOne();
        dispatcher.dispatchPending();
        deliverAgain(theDeliveryToNotifyResponsibles());

        dispatcher.dispatchPending();

        notificationsOf(expedition).andExpect(jsonPath("$.length()").value(1));
        final var delivery = theDeliveryToNotifyResponsibles();
        assertEquals(DeliveryStatus.PENDING.name(), delivery.getStatus());
        assertEquals(2, delivery.getAttempts());
        assertNotNull(delivery.getLastError());
    }

    private EventDeliveryEntity theDeliveryToNotifyResponsibles() {
        return deliveries.findAll().stream()
                .filter(delivery -> delivery.getId().getConsumer().equals(NOTIFY_RESPONSIBLES))
                .findFirst().orElseThrow();
    }

    private void deliverAgain(EventDeliveryEntity delivery) {
        delivery.setStatus(DeliveryStatus.PENDING.name());
        delivery.setNextAttemptAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        deliveries.save(delivery);
    }

    @Test
    void answersNotFoundForAnExpeditionThatDoesNotExist() throws Exception {
        api.perform(get("/api/expeditions/{id}/notifications", "e-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no expedition with id e-404"));
    }
}
