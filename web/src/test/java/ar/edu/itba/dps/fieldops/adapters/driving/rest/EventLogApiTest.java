package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/clean-database.sql")
class EventLogApiTest {
    @Autowired
    private MockMvc api;

    @Test
    void listsWhatHappenedInTheExpeditionInTheOrderTheSystemRecordedIt() throws Exception {
        final var expedition = anExpeditionInExecutionWithTheSampleCollectionStarted();
        registerIncident(expedition).andExpect(status().isCreated());
        finishTheStartedActivity(expedition).andExpect(status().isNoContent());

        api.perform(get("/api/expeditions/{id}/events", expedition))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].type").value(Matchers.contains(
                        "ExpeditionApproved", "IncidentRegistered", "ActivityCompleted")))
                .andExpect(jsonPath("$[1].occurredAt").value("2026-10-01T13:30:00"))
                .andExpect(jsonPath("$[2].occurredAt").value("2026-10-01T14:00:00"))
                .andExpect(jsonPath("$[0].id").value(Matchers.not(Matchers.emptyOrNullString())))
                .andExpect(jsonPath("$[0].recordedAt").value(Matchers.not(Matchers.emptyOrNullString())));
    }

    @Test
    void showsThatAConsumerStillHasToReceiveAnEventTheDispatcherHasNotDelivered() throws Exception {
        final var expedition = anExpeditionInExecutionWithTheSampleCollectionStarted();

        api.perform(get("/api/expeditions/{id}/events", expedition))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("ExpeditionApproved"))
                .andExpect(jsonPath("$[0].deliveries.length()").value(1))
                .andExpect(jsonPath("$[0].deliveries[0].consumer").value("notify-responsibles"))
                .andExpect(jsonPath("$[0].deliveries[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].deliveries[0].attempts").value(0));
    }

    @Test
    void answersAnEmptyTimelineForAnExpeditionThatHasNotDoneAnythingYet() throws Exception {
        final var expedition = aDraftExpedition();

        api.perform(get("/api/expeditions/{id}/events", expedition))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void answersNotFoundForAnExpeditionThatDoesNotExist() throws Exception {
        api.perform(get("/api/expeditions/{id}/events", "e-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no expedition with id e-404"));
    }

    private ResultActions registerIncident(String expeditionId) throws Exception {
        return api.perform(post("/api/expeditions/{expeditionId}/incidents", expeditionId)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "activityId": "a-1",
                          "description": "The cooler stopped working and Lee went back to base",
                          "occurredAt": "2026-10-01T13:30:00",
                          "resourceId": "p-2",
                          "expectedDelayMinutes": 120
                        }
                        """));
    }

    private ResultActions finishTheStartedActivity(String expeditionId) throws Exception {
        return api.perform(post("/api/expeditions/{expeditionId}/activities/{activityId}/completion", expeditionId, "a-1")
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"at": "2026-10-01T14:00:00", "result": "Nothing collected: the cooler failed"}
                        """));
    }

    private ResultActions transition(String expeditionId, String subResource) throws Exception {
        return api.perform(post("/api/expeditions/{expeditionId}/{sub}", expeditionId, subResource)
                .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions register(String path, String body) throws Exception {
        return api.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String aDraftExpedition() throws Exception {
        register("/api/zones", """
                {"id": "z-1", "name": "Coastal reef", "requiredPermits": []}
                """).andExpect(status().isCreated());
        register("/api/staff", """
                {"id": "p-1", "name": "Ada", "certifications": []}
                """).andExpect(status().isCreated());
        final var created = api.perform(post("/api/expeditions").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "name": "Coastal survey",
                          "period": {"start": "2026-10-01T00:00:00", "end": "2026-10-02T00:00:00"},
                          "objectives": ["Map the coastal reef"],
                          "zoneIds": ["z-1"],
                          "responsibleIds": ["p-1"],
                          "grantedPermits": [],
                          "restrictions": {"maxParticipants": 10, "riskTolerance": "MEDIUM"}
                        }
                        """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(created, "$.id");
    }

    private String anExpeditionInExecutionWithTheSampleCollectionStarted() throws Exception {
        final var expedition = aDraftExpedition();
        register("/api/staff", """
                {"id": "p-2", "name": "Lee", "certifications": []}
                """).andExpect(status().isCreated());
        register("/api/instruments", """
                {"id": "i-1", "name": "Field cooler", "category": "Cooler", "certifications": []}
                """).andExpect(status().isCreated());
        register("/api/supplies", """
                {"id": "s-1", "name": "Sample containers", "category": "Sample container",
                 "initialStock": {"amount": "5", "unit": "UNITS"}}
                """).andExpect(status().isCreated());

        api.perform(post("/api/expeditions/{expeditionId}/activities", expedition)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {
                                  "id": "a-1",
                                  "name": "Sample collection a-1",
                                  "timeWindow": {"start": "2026-10-01T08:00:00", "end": "2026-10-01T18:00:00"},
                                  "scheduledPeriod": {"start": "2026-10-01T13:00:00", "end": "2026-10-01T15:00:00"},
                                  "zoneId": "z-1",
                                  "dependencyIds": [],
                                  "type": "Sample collection",
                                  "parameters": {"sampleCount": 1},
                                  "atNight": false
                                }
                                """))
                .andExpect(status().isCreated());
        for (final var assignment : new String[][] {{"staff", "p-2"}, {"equipment", "i-1"}, {"supplies", "s-1"}}) {
            api.perform(post("/api/expeditions/{expeditionId}/activities/{activityId}/{kind}",
                            expedition, "a-1", assignment[0])
                            .contentType(MediaType.APPLICATION_JSON).content("""
                                    {"resourceId": "%s"}
                                    """.formatted(assignment[1])))
                    .andExpect(status().isNoContent());
        }

        transition(expedition, "review").andExpect(status().isNoContent());
        api.perform(post("/api/expeditions/{expeditionId}/approval", expedition)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isNoContent());
        transition(expedition, "execution").andExpect(status().isNoContent());
        api.perform(post("/api/expeditions/{expeditionId}/activities/{activityId}/start", expedition, "a-1")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"at": "2026-10-01T13:00:00"}
                                """))
                .andExpect(status().isNoContent());
        return expedition;
    }
}
