package ar.edu.itba.dps.fieldops.business.integration;

import ar.edu.itba.dps.fieldops.adapters.driven.EventDispatcher;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(DispatcherTestConfiguration.class)
@Sql("/clean-database.sql")
class NotificationsApiTest {
    @Autowired
    private MockMvc api;
    @Autowired
    private ApprovedExpeditions approvedExpeditions;
    @Autowired
    private EventDispatcher dispatcher;

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
    void answersNotFoundForAnExpeditionThatDoesNotExist() throws Exception {
        api.perform(get("/api/expeditions/{id}/notifications", "e-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no expedition with id e-404"));
    }
}
