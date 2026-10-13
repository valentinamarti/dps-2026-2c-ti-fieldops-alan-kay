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

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ExpeditionPlanApiTest {
    @Autowired
    private MockMvc api;

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
    void theValidationOfAnApprovedExpeditionIgnoresTheReservationsItsOwnApprovalMade() throws Exception {
        final var expedition = anExpeditionWithASampleCollectionFullyResourced();

        api.perform(post("/api/expeditions/{id}/review", expedition)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        api.perform(post("/api/expeditions/{id}/approval", expedition)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNoContent());

        api.perform(get("/api/expeditions/{id}/validation", expedition))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canBeApproved").value(true))
                .andExpect(jsonPath("$.criticals").value(Matchers.empty()))
                .andExpect(jsonPath("$.warnings").value(Matchers.empty()));
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
        return assign(expeditionId, activityId, "staff", personId);
    }

    private ResultActions assign(String expeditionId, String activityId, String kind, String resourceId) throws Exception {
        return api.perform(post("/api/expeditions/{expeditionId}/activities/{activityId}/{kind}",
                        expeditionId, activityId, kind)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"resourceId": "%s"}
                        """.formatted(resourceId)));
    }

    private String anExpeditionWithASampleCollectionFullyResourced() throws Exception {
        final var expedition = anExpeditionInTheCoastalReef();
        api.perform(post("/api/staff").contentType(MediaType.APPLICATION_JSON).content("""
                        {"id": "p-2", "name": "Lee", "certifications": []}
                        """))
                .andExpect(status().isCreated());
        api.perform(post("/api/instruments").contentType(MediaType.APPLICATION_JSON).content("""
                        {"id": "i-1", "name": "Field cooler", "category": "Cooler", "certifications": []}
                        """))
                .andExpect(status().isCreated());
        api.perform(post("/api/supplies").contentType(MediaType.APPLICATION_JSON).content("""
                        {"id": "s-1", "name": "Sample containers", "category": "Sample container",
                         "initialStock": {"amount": "5", "unit": "UNITS"}}
                        """))
                .andExpect(status().isCreated());

        scheduleActivity(expedition, activity("a-1", 8, 10, "Sample collection", "{\"sampleCount\": 1}"))
                .andExpect(status().isCreated());
        assign(expedition, "a-1", "staff", "p-2").andExpect(status().isNoContent());
        assign(expedition, "a-1", "equipment", "i-1").andExpect(status().isNoContent());
        assign(expedition, "a-1", "supplies", "s-1").andExpect(status().isNoContent());
        return expedition;
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
