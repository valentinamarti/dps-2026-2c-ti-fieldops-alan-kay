package ar.edu.itba.dps.fieldops.adapters.driving.seed;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DemoScriptApiTest {
    private static final String NIGHT_DIVE = """
            {
              "id": "a-10",
              "name": "Night dive at the reef",
              "timeWindow": {"start": "2026-11-05T18:00:00", "end": "2026-11-06T06:00:00"},
              "scheduledPeriod": {"start": "2026-11-05T20:00:00", "end": "2026-11-05T22:00:00"},
              "zoneId": "z-coast",
              "dependencyIds": ["a-09"],
              "type": "Diving",
              "parameters": {"maxDepthMeters": 35},
              "atNight": true
            }
            """;

    private static final String ACCEPTING_THE_RISK = """
            {
              "acceptedWarnings": [
                {
                  "rule": "risk-tolerance",
                  "subject": {"activityId": "a-10"},
                  "responsibleId": "p-ada",
                  "justification": "Two divers certified for night operations, a floodlight and a boat on site"
                }
              ]
            }
            """;

    @Autowired
    private MockMvc api;

    @Test
    void theSeedLeavesTheCatalogTheZonesAndOneExpeditionInDraft() throws Exception {
        api.perform(get("/api/activity-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[*].name").value(Matchers.containsInAnyOrder("sample collection", "diving",
                        "drone survey", "wildlife observation", "water quality measurement", "transfer")));
        api.perform(get("/api/zones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(Matchers.contains("z-coast", "z-wetland")));
        api.perform(get("/api/staff")).andExpect(jsonPath("$.length()").value(8));
        api.perform(get("/api/equipment"))
                .andExpect(jsonPath("$.vehicles.length()").value(2))
                .andExpect(jsonPath("$.instruments.length()").value(9));
        api.perform(get("/api/supplies")).andExpect(jsonPath("$.length()").value(3));

        api.perform(get("/api/expeditions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Patagonia coastal survey"))
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andExpect(jsonPath("$[0].grantedPermits").value(Matchers.contains("Marine research",
                        "Wetland access", "night operations in zone z-coast")));
    }

    @Test
    void theSeededItineraryHasTheParallelBlockInsideTheSequentialOneAndNothingAgainstIt() throws Exception {
        final var expedition = theDemoExpedition();

        api.perform(get("/api/expeditions/{id}/itinerary", expedition))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes.length()").value(7))
                .andExpect(jsonPath("$.nodes[0].activity.id").value("a-01"))
                .andExpect(jsonPath("$.nodes[1].type").value("sequential"))
                .andExpect(jsonPath("$.nodes[1].estimatedDurationMinutes").value(90 + 210 + 180))
                .andExpect(jsonPath("$.nodes[1].children.length()").value(3))
                .andExpect(jsonPath("$.nodes[1].children[0].activity.id").value("a-02"))
                .andExpect(jsonPath("$.nodes[1].children[1].type").value("parallel"))
                .andExpect(jsonPath("$.nodes[1].children[1].estimatedDurationMinutes").value(210))
                .andExpect(jsonPath("$.nodes[1].children[1].children[*].activity.id")
                        .value(Matchers.contains("a-03", "a-04")))
                .andExpect(jsonPath("$.nodes[1].children[2].activity.id").value("a-05"));

        api.perform(get("/api/expeditions/{id}/estimate", expedition))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDurationMinutes").value(1475))
                .andExpect(jsonPath("$.overallRisk").value("MEDIUM"));

        api.perform(get("/api/expeditions/{id}/validation", expedition))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canBeApproved").value(true))
                .andExpect(jsonPath("$.criticals").value(Matchers.empty()))
                .andExpect(jsonPath("$.warnings").value(Matchers.empty()));
    }

    @Test
    void walksTheScriptOfTheDemoFromTheNightActivityToTheAuditOfTheReplanning() throws Exception {
        final var expedition = theDemoExpedition();

        api.perform(post("/api/expeditions/{id}/activities", expedition)
                        .contentType(MediaType.APPLICATION_JSON).content(NIGHT_DIVE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("a-10"))
                .andExpect(jsonPath("$.estimatedDurationMinutes").value(45 + 2 * 35))
                .andExpect(jsonPath("$.estimatedRisk").value("EXTREME"))
                .andExpect(jsonPath("$.requiredEquipment[*].category")
                        .value(Matchers.contains("boat", "oxygen tank", "lighting")))
                .andExpect(jsonPath("$.requiredCertifications").value(Matchers.contains("night operations")))
                .andExpect(jsonPath("$.requiredPermits")
                        .value(Matchers.contains("night operations in zone z-coast")));

        api.perform(get("/api/expeditions/{id}/activities/{activityId}/candidates", expedition, "a-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.staff[0].candidates[*].id")
                        .value(Matchers.containsInAnyOrder("p-ada", "p-grace")))
                .andExpect(jsonPath("$.equipment[2].requirement.category").value("lighting"))
                .andExpect(jsonPath("$.equipment[2].instruments[*].id").value(Matchers.contains("i-lamp-1")));

        assign(expedition, "staff", "p-ada");
        assign(expedition, "staff", "p-grace");
        assign(expedition, "equipment", "v-boat-1");
        assign(expedition, "equipment", "i-tank-1");
        assign(expedition, "equipment", "i-tank-2");
        assign(expedition, "supplies", "s-fuel");

        api.perform(get("/api/expeditions/{id}/validation", expedition))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canBeApproved").value(false))
                .andExpect(jsonPath("$.criticals.length()").value(1))
                .andExpect(jsonPath("$.criticals[0].rule").value("missing-resources"))
                .andExpect(jsonPath("$.criticals[0].subject.activityId").value("a-10"))
                .andExpect(jsonPath("$.criticals[0].message")
                        .value("Night dive at the reef has 0 of the 1 lighting it needs"))
                .andExpect(jsonPath("$.warnings.length()").value(1))
                .andExpect(jsonPath("$.warnings[0].rule").value("risk-tolerance"))
                .andExpect(jsonPath("$.warnings[0].subject.activityId").value("a-10"))
                .andExpect(jsonPath("$.warnings[0].message")
                        .value("Night dive at the reef has EXTREME risk, above the expedition tolerance of HIGH"));

        transition(expedition, "review").andExpect(status().isNoContent());
        api.perform(post("/api/expeditions/{id}/approval", expedition)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.criticals.length()").value(1))
                .andExpect(jsonPath("$.criticals[0].rule").value("missing-resources"))
                .andExpect(jsonPath("$.detail")
                        .value(Matchers.containsString("Night dive at the reef has 0 of the 1 lighting it needs")));
        assertStatus(expedition, "IN_REVIEW");

        transition(expedition, "draft").andExpect(status().isNoContent());
        assign(expedition, "equipment", "i-lamp-1");

        api.perform(get("/api/expeditions/{id}/validation", expedition))
                .andExpect(jsonPath("$.canBeApproved").value(true))
                .andExpect(jsonPath("$.criticals").value(Matchers.empty()))
                .andExpect(jsonPath("$.warnings.length()").value(1));
        api.perform(get("/api/expeditions/{id}/estimate", expedition))
                .andExpect(jsonPath("$.totalDurationMinutes").value(1475 + 115))
                .andExpect(jsonPath("$.overallRisk").value("EXTREME"));

        transition(expedition, "review").andExpect(status().isNoContent());
        approve(expedition).andExpect(status().isNoContent());
        assertStatus(expedition, "APPROVED");

        transition(expedition, "execution").andExpect(status().isNoContent());
        api.perform(post("/api/expeditions/{id}/activities/{activityId}/start", expedition, "a-08")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"at": "2026-11-04T11:30:00"}
                                """))
                .andExpect(status().isNoContent());
        api.perform(post("/api/expeditions/{id}/activities/{activityId}/observations", expedition, "a-08")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"note": "The water level rose and the first station is under water",
                                 "recordedAt": "2026-11-04T11:55:00"}
                                """))
                .andExpect(status().isNoContent());

        final var registered = api.perform(post("/api/expeditions/{id}/incidents", expedition)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {
                                  "activityId": "a-08",
                                  "description": "A storm flooded the station and Lee went back to the camp",
                                  "occurredAt": "2026-11-04T12:10:00",
                                  "resourceId": "p-lee",
                                  "expectedDelayMinutes": 120
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.originalExpeditionId").value(expedition))
                .andExpect(jsonPath("$.incident.id").value(expedition + "/incidents/1"))
                .andExpect(jsonPath("$.incident.expectedDelayMinutes").value(120))
                .andExpect(jsonPath("$.options[*].number").value(Matchers.contains(1, 2, 3, 4, 5)))
                .andExpect(jsonPath("$.options[0].summary").value("Run Wetland sample collection 2 h later "
                        + "together with Wetland aerial survey, Night dive at the reef, Transfer back to base"))
                .andExpect(jsonPath("$.options[*].summary").value(Matchers.hasItems(
                        "Replace p-lee with p-max in Wetland sample collection",
                        "Replace p-lee with p-emmy in Wetland sample collection",
                        "Cancel Wetland sample collection and what depends on it: Wetland aerial survey, "
                                + "Night dive at the reef, Transfer back to base")))
                .andReturn().getResponse().getContentAsString();
        final String proposal = JsonPath.read(registered, "$.id");

        api.perform(get("/api/expeditions/{id}/replan-proposals", expedition))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(proposal));
        api.perform(get("/api/replan-proposals/{proposalId}", proposal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.decision").value(Matchers.nullValue()));

        api.perform(post("/api/expeditions/{id}/activities/{activityId}/completion", expedition, "a-08")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"at": "2026-11-04T12:40:00",
                                 "result": "One of the four samples collected before the station flooded"}
                                """))
                .andExpect(status().isNoContent());

        final var accepted = api.perform(post("/api/replan-proposals/{proposalId}/acceptance", proposal)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"optionNumber": 1, "responsibleId": "p-ada",
                                 "justification": "The water goes down in two hours and the crew stays on site",
                                 "decidedAt": "2026-11-04T12:45:00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.name").value("Patagonia coastal survey"))
                .andExpect(jsonPath("$.replacedExpeditionId").value(expedition))
                .andExpect(jsonPath("$.replacementId").value(Matchers.nullValue()))
                .andReturn().getResponse().getContentAsString();
        final String alternative = JsonPath.read(accepted, "$.id");
        assertStatus(expedition, "SUSPENDED");

        api.perform(get("/api/expeditions/{id}/itinerary", alternative))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes.length()").value(8))
                .andExpect(jsonPath("$.nodes[1].type").value("sequential"))
                .andExpect(jsonPath("$.nodes[1].children[1].type").value("parallel"))
                .andExpect(jsonPath("$.nodes[4].activity.id").value("a-08"))
                .andExpect(jsonPath("$.nodes[4].activity.scheduledPeriod.start").value("2026-11-04T13:30:00"))
                .andExpect(jsonPath("$.nodes[4].activity.scheduledPeriod.end").value("2026-11-04T15:50:00"))
                .andExpect(jsonPath("$.nodes[5].activity.id").value("a-09"))
                .andExpect(jsonPath("$.nodes[5].activity.scheduledPeriod.start").value("2026-11-04T17:00:00"))
                .andExpect(jsonPath("$.nodes[6].activity.id").value("a-10"))
                .andExpect(jsonPath("$.nodes[6].activity.scheduledPeriod.start").value("2026-11-05T22:00:00"))
                .andExpect(jsonPath("$.nodes[7].activity.id").value("a-11"))
                .andExpect(jsonPath("$.nodes[7].activity.scheduledPeriod.start").value("2026-11-06T10:00:00"));

        transition(alternative, "review").andExpect(status().isNoContent());
        approve(alternative).andExpect(status().isNoContent());

        api.perform(get("/api/expeditions/{id}", expedition))
                .andExpect(jsonPath("$.status").value("REPLACED"))
                .andExpect(jsonPath("$.replacementId").value(alternative));

        api.perform(get("/api/replan-proposals/{proposalId}", proposal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.originalExpeditionId").value(expedition))
                .andExpect(jsonPath("$.incident.activityId").value("a-08"))
                .andExpect(jsonPath("$.incident.resourceId").value("p-lee"))
                .andExpect(jsonPath("$.options.length()").value(5))
                .andExpect(jsonPath("$.decision.chosenOptionNumber").value(1))
                .andExpect(jsonPath("$.decision.responsibleId").value("p-ada"))
                .andExpect(jsonPath("$.decision.alternativeExpeditionId").value(alternative));
        api.perform(get("/api/expeditions/{id}/itinerary", expedition))
                .andExpect(jsonPath("$.nodes[4].activity.scheduledPeriod.start").value("2026-11-04T11:30:00"));

        api.perform(get("/api/expeditions/{id}/report", expedition))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduledActivities").value(11))
                .andExpect(jsonPath("$.finishedActivities").value(1))
                .andExpect(jsonPath("$.incidents.length()").value(1))
                .andExpect(jsonPath("$.acceptedWarnings.length()").value(1))
                .andExpect(jsonPath("$.acceptedWarnings[0].justification").value(
                        "Two divers certified for night operations, a floodlight and a boat on site"));
        api.perform(get("/api/expeditions/{id}/report", alternative))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduledActivities").value(11))
                .andExpect(jsonPath("$.incidents.length()").value(1))
                .andExpect(jsonPath("$.incidents[0].id").value(expedition + "/incidents/1"));
    }

    private String theDemoExpedition() throws Exception {
        final var expeditions = api.perform(get("/api/expeditions"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(expeditions, "$[0].id");
    }

    private void assign(String expedition, String kind, String resourceId) throws Exception {
        api.perform(post("/api/expeditions/{id}/activities/{activityId}/{kind}", expedition, "a-10", kind)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"resourceId": "%s"}
                                """.formatted(resourceId)))
                .andExpect(status().isNoContent());
    }

    private ResultActions transition(String expedition, String subResource) throws Exception {
        return api.perform(post("/api/expeditions/{id}/{sub}", expedition, subResource)
                .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions approve(String expedition) throws Exception {
        return api.perform(post("/api/expeditions/{id}/approval", expedition)
                .contentType(MediaType.APPLICATION_JSON).content(ACCEPTING_THE_RISK));
    }

    private void assertStatus(String expedition, String expected) throws Exception {
        api.perform(get("/api/expeditions/{id}", expedition))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(expected));
    }
}
