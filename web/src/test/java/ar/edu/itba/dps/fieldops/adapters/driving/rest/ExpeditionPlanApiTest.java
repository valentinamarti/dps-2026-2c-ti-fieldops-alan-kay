package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validating and estimating an expedition over HTTP, without approving it. It covers the second acceptance
 * criterion of F2 (the duration of a sequential block is the sum of its parts and of a parallel one, the
 * maximum) through {@code GET /estimate}, and the third (the resource conflict between parallel activities of
 * the same block) through {@code GET /validation}, now that {@code AssignmentController} can assign the
 * shared resource that triggers it.
 *
 * <p>The whole wiring of {@code FieldOpsConfig} with the in-memory adapters and no mocks (D37).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ExpeditionPlanApiTest {

    @Autowired
    private MockMvc api;

    /**
     * The acceptance criterion of F2 over HTTP: a sequential block with a parallel one inside sums the
     * sequential parts and takes the maximum of the parallel branches (D39).
     */
    @Test
    void theEstimateAddsASequentialBlockAndTakesTheMaximumOfAParallelOne() throws Exception {
        final var expedition = anExpeditionInTheCoastalReef();

        api.perform(post("/api/expeditions/{expeditionId}/blocks", expedition)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "type": "sequential",
                          "children": [
                            {"activity": %s},
                            {
                              "type": "parallel",
                              "children": [
                                {"activity": %s},
                                {"activity": %s}
                              ]
                            }
                          ]
                        }
                        """.formatted(
                        activity("a-1", 8, 10, "Sample collection", "{\"sampleCount\": 3}"),
                        activity("a-2", 10, 11, "Transfer", "{\"distanceKilometers\": 10}"),
                        activity("a-3", 10, 12, "Drone survey", "{\"areaHectares\": 20}"))))
                .andExpect(status().isCreated());

        // 120 (sampling) + max(50, 90) (transfer vs. drone survey) = 210.
        api.perform(get("/api/expeditions/{id}/estimate", expedition))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDurationMinutes").value(210))
                .andExpect(jsonPath("$.overallRisk").value("LOW"));
    }

    @Test
    void theEstimateOfAnExpeditionWithoutAnythingScheduledIsZero() throws Exception {
        api.perform(get("/api/expeditions/{id}/estimate", anExpeditionInTheCoastalReef()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDurationMinutes").value(0))
                .andExpect(jsonPath("$.estimatedConsumption").isEmpty());
    }

    @Test
    void answersNotFoundWhenEstimatingAnExpeditionThatDoesNotExist() throws Exception {
        api.perform(get("/api/expeditions/{id}/estimate", "e-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no expedition with id e-404"));
    }

    /**
     * The validation reaches the same rules over HTTP, with criticals and warnings apart (D22, D32): an
     * activity with nothing assigned is missing every resource it needs, which does not require an endpoint
     * for assigning them.
     */
    @Test
    void theValidationReportsWhatIsMissingAsACriticalAndSaysItCannotBeApproved() throws Exception {
        final var expedition = anExpeditionInTheCoastalReef();
        scheduleActivity(expedition, activity("a-1", 8, 10, "Sample collection", "{\"sampleCount\": 3}"))
                .andExpect(status().isCreated());

        api.perform(get("/api/expeditions/{id}/validation", expedition))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canBeApproved").value(false))
                .andExpect(jsonPath("$.criticals[?(@.rule == 'missing-resources')].subject.activityId")
                        .value(Matchers.everyItem(Matchers.is("a-1"))))
                .andExpect(jsonPath("$.warnings").isEmpty());
    }

    /**
     * The third acceptance criterion of F2 over HTTP: two activities of the same parallel block cannot share a
     * reusable resource, not even when (as here) their periods overlap outright (D41). It needs a resource
     * assigned to each activity, which {@code AssignmentController} now exposes.
     */
    @Test
    void theValidationReportsAResourceConflictBetweenParallelActivitiesOfTheSameBlock() throws Exception {
        final var expedition = anExpeditionInTheCoastalReef();
        api.perform(post("/api/staff").contentType(MediaType.APPLICATION_JSON).content("""
                        {"id": "p-2", "name": "Lee", "certifications": []}
                        """))
                .andExpect(status().isCreated());

        api.perform(post("/api/expeditions/{expeditionId}/blocks", expedition)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "type": "parallel",
                          "children": [
                            {"activity": %s},
                            {"activity": %s}
                          ]
                        }
                        """.formatted(
                        activity("a-1", 9, 11, "Sample collection", "{\"sampleCount\": 1}"),
                        activity("a-2", 9, 11, "Sample collection", "{\"sampleCount\": 1}"))))
                .andExpect(status().isCreated());

        assign(expedition, "a-1", "p-2").andExpect(status().isNoContent());
        assign(expedition, "a-2", "p-2").andExpect(status().isNoContent());

        api.perform(get("/api/expeditions/{id}/validation", expedition))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canBeApproved").value(false))
                .andExpect(jsonPath("$.criticals[?(@.rule == 'resource-overlap')].message")
                        .value(Matchers.hasItem("p-2 is assigned to Activity a-1 and Activity a-2 at the same time")))
                .andExpect(jsonPath("$.criticals[?(@.rule == 'resource-overlap')].subject.resourceId")
                        .value(Matchers.everyItem(Matchers.is("p-2"))));
    }

    @Test
    void answersNotFoundWhenValidatingAnExpeditionThatDoesNotExist() throws Exception {
        api.perform(get("/api/expeditions/{id}/validation", "e-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no expedition with id e-404"));
    }

    private ResultActions scheduleActivity(String expeditionId, String body) throws Exception {
        return api.perform(post("/api/expeditions/{expeditionId}/activities", expeditionId)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions assign(String expeditionId, String activityId, String personId) throws Exception {
        return api.perform(post("/api/expeditions/{expeditionId}/activities/{activityId}/staff",
                        expeditionId, activityId)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"resourceId": "%s"}
                        """.formatted(personId)));
    }

    private static String activity(String id, int fromHour, int toHour, String type, String parameters) {
        return """
                {
                  "id": "%s",
                  "name": "Activity %s",
                  "timeWindow": {"start": "2026-10-01T00:00:00", "end": "2026-10-02T00:00:00"},
                  "scheduledPeriod": {"start": "2026-10-01T%02d:00:00", "end": "2026-10-01T%02d:00:00"},
                  "zoneId": "z-1",
                  "dependencyIds": [],
                  "type": "%s",
                  "parameters": %s,
                  "atNight": false
                }
                """.formatted(id, id, fromHour, toHour, type, parameters);
    }

    private String anExpeditionInTheCoastalReef() throws Exception {
        api.perform(post("/api/zones").contentType(MediaType.APPLICATION_JSON).content("""
                        {"id": "z-1", "name": "Coastal reef", "requiredPermits": []}
                        """))
                .andExpect(status().isCreated());
        api.perform(post("/api/staff").contentType(MediaType.APPLICATION_JSON).content("""
                        {"id": "p-1", "name": "Ada", "certifications": []}
                        """))
                .andExpect(status().isCreated());

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
        return JsonPath.read(created, "$.id").toString();
    }
}
