package ar.edu.itba.dps.fieldops.business.integration;

import ar.edu.itba.dps.fieldops.adapters.driven.EventDispatcher;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(DispatcherTestConfiguration.class)
@Sql("/clean-database.sql")
class ReportWithEventsApiTest {
    private static final ActivityId ACTIVITY = new ActivityId("a-1");
    private static final LocalDateTime THIRTEEN = LocalDateTime.of(2026, 10, 1, 13, 0);

    @Autowired
    private MockMvc api;
    @Autowired
    private ApprovedExpeditions approvedExpeditions;
    @Autowired
    private TransitionExpeditionPort transitions;
    @Autowired
    private TrackActivityPort tracking;
    @Autowired
    private EventDispatcher dispatcher;

    private ExpeditionId anExpeditionWithItsActivityFinished() {
        final var expedition = approvedExpeditions.approveOne();
        transitions.start(expedition);
        tracking.start(expedition, ACTIVITY, THIRTEEN);
        tracking.finish(expedition, ACTIVITY, THIRTEEN.plusHours(1), "Twelve samples collected");
        return expedition;
    }

    private ResultActions reportOf(ExpeditionId expedition) throws Exception {
        return api.perform(get("/api/expeditions/{id}/report", expedition.value()));
    }

    @Test
    void theReportShowsTheProgressOnceTheDispatcherDeliversTheEvent() throws Exception {
        final var expedition = anExpeditionWithItsActivityFinished();

        dispatcher.dispatchPending();

        reportOf(expedition)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress.length()").value(1))
                .andExpect(jsonPath("$.progress[0].activityId").value("a-1"))
                .andExpect(jsonPath("$.progress[0].occurredAt").value("2026-10-01T14:00:00"))
                .andExpect(jsonPath("$.progress[0].finishedActivities").value(1))
                .andExpect(jsonPath("$.progress[0].scheduledActivities").value(1));
    }

    @Test
    void beforeTheDispatcherRunsTheReportAlreadyKnowsTheStateButNotTheHistory() throws Exception {
        final var expedition = anExpeditionWithItsActivityFinished();

        reportOf(expedition)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finishedActivities").value(1))
                .andExpect(jsonPath("$.progress.length()").value(0));
    }

    @Test
    void theReportIncludesTheTimelineOfEventsInTheOrderTheSystemRecordedThem() throws Exception {
        final var expedition = anExpeditionWithItsActivityFinished();

        reportOf(expedition)
                .andExpect(jsonPath("$.timeline[*].type").value(Matchers.contains("ExpeditionApproved", "ActivityCompleted")))
                .andExpect(jsonPath("$.timeline[1].deliveries[*].consumer").value(
                        Matchers.hasItems("notify-responsibles", "track-progress")));
    }
}
