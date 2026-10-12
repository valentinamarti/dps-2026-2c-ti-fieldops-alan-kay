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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Scheduling a night activity over HTTP, from an empty application: the zone, the person with the night
 * certification and the lighting go in through the catalog, the expedition grants the special permit of the
 * zone and the activity is asked for by the name of its type. It is the acceptance criterion of F1 that until
 * the catalog and the zones had endpoints could only be exercised from Java.
 *
 * <p>The whole wiring of {@code FieldOpsConfig} with the in-memory adapters and no mocks; the repositories are
 * shared beans, so each test starts with a new context (D37).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ActivityApiTest {

    private static final String NIGHT_SAMPLING = """
            {
              "id": "a-1",
              "name": "Night sampling",
              "timeWindow": {"start": "2026-10-01T18:00:00", "end": "2026-10-02T00:00:00"},
              "scheduledPeriod": {"start": "2026-10-01T20:00:00", "end": "2026-10-01T22:00:00"},
              "zoneId": "z-1",
              "dependencyIds": [],
              "type": "Sample collection",
              "parameters": {"sampleCount": 3},
              "atNight": true
            }
            """;

    @Autowired
    private MockMvc api;

    /**
     * What the night variant adds travels in the answer as what the activity demands and not as a flag, because
     * a wrapped rule does not say that it is nocturnal (D42): the lighting, the certification of everyone
     * assigned, the permit of the zone with the name the expedition has to grant, and one more level of risk
     * over the {@code LOW} of a sample collection.
     */
    @Test
    void schedulesANightActivityAskedForByItsTypeAndAnswersWhatItDemands() throws Exception {
        final var expedition = anExpeditionInTheCoastalReef();

        scheduleIn(expedition, NIGHT_SAMPLING)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("a-1"))
                .andExpect(jsonPath("$.name").value("Night sampling"))
                .andExpect(jsonPath("$.zoneId").value("z-1"))
                .andExpect(jsonPath("$.scheduledPeriod.start").value("2026-10-01T20:00:00"))
                .andExpect(jsonPath("$.estimatedDurationMinutes").value(120))
                .andExpect(jsonPath("$.estimatedRisk").value("MEDIUM"))
                .andExpect(jsonPath("$.requiredCertifications").value("night operations"))
                .andExpect(jsonPath("$.requiredPermits").value("night operations in zone z-1"))
                .andExpect(jsonPath("$.requiredEquipment[?(@.category == 'lighting')].count").value(1))
                .andExpect(jsonPath("$.requiredEquipment[?(@.category == 'cooler')].count").value(1))
                .andExpect(jsonPath("$.requiredSupplies[0].category").value("sample container"))
                .andExpect(jsonPath("$.requiredSupplies[0].quantity.amount").value("3"));
    }

    /**
     * That the night activity stayed in the itinerary is read through another activity that depends on it: the
     * dependency is resolved among the ones already scheduled in the same expedition, so a second activity
     * that declares it could not be scheduled if the first one were not there (D53).
     */
    @Test
    void theNightActivityStaysInTheItineraryAndCanBeDependedOn() throws Exception {
        final var expedition = anExpeditionInTheCoastalReef();
        scheduleIn(expedition, NIGHT_SAMPLING).andExpect(status().isCreated());

        scheduleIn(expedition, """
                {
                  "id": "a-2",
                  "name": "Camp transfer",
                  "timeWindow": {"start": "2026-10-01T18:00:00", "end": "2026-10-02T00:00:00"},
                  "scheduledPeriod": {"start": "2026-10-01T22:00:00", "end": "2026-10-01T23:00:00"},
                  "zoneId": "z-1",
                  "dependencyIds": ["a-1"],
                  "type": "Transfer",
                  "parameters": {"distanceKilometers": 10},
                  "atNight": false
                }
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dependencyIds[0]").value("a-1"))
                .andExpect(jsonPath("$.estimatedRisk").value("LOW"))
                .andExpect(jsonPath("$.estimatedDurationMinutes").value(50))
                .andExpect(jsonPath("$.requiredStaff[0].headcount").value(1))
                .andExpect(jsonPath("$.requiredStaff[0].certifications").value("off-road driving"))
                .andExpect(jsonPath("$.requiredCertifications").isEmpty())
                .andExpect(jsonPath("$.requiredPermits").isEmpty());
    }

    @Test
    void answersBadRequestForATypeThatDoesNotExist() throws Exception {
        final var expedition = anExpeditionInTheCoastalReef();

        scheduleIn(expedition, NIGHT_SAMPLING.replace("\"Sample collection\"", "\"Spelunking\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(Matchers.startsWith("There is no activity type named spelunking")));
    }

    @Test
    void answersBadRequestForAParameterTheTypeDoesNotAccept() throws Exception {
        final var expedition = anExpeditionInTheCoastalReef();

        scheduleIn(expedition, NIGHT_SAMPLING.replace("\"sampleCount\": 3", "\"sampleCount\": 0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "An activity of type sample collection cannot be created: "
                                + "sampleCount cannot be 0, sample count must be positive"));
    }

    @Test
    void answersBadRequestWhenTheParameterOfTheTypeIsMissing() throws Exception {
        final var expedition = anExpeditionInTheCoastalReef();

        scheduleIn(expedition, NIGHT_SAMPLING.replace("{\"sampleCount\": 3}", "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "An activity of type sample collection cannot be created: the parameter sampleCount is required"));
    }

    @Test
    void answersNotFoundForAZoneThatIsNotRegistered() throws Exception {
        final var expedition = anExpeditionInTheCoastalReef();

        scheduleIn(expedition, NIGHT_SAMPLING.replace("\"zoneId\": \"z-1\"", "\"zoneId\": \"z-404\""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no zone with id z-404"));
    }

    @Test
    void answersNotFoundForADependencyThatNobodyScheduled() throws Exception {
        final var expedition = anExpeditionInTheCoastalReef();

        scheduleIn(expedition, NIGHT_SAMPLING.replace("\"dependencyIds\": []", "\"dependencyIds\": [\"a-404\"]"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no scheduled activity with id a-404"));
    }

    @Test
    void answersNotFoundForAnExpeditionThatDoesNotExist() throws Exception {
        scheduleIn("e-404", NIGHT_SAMPLING)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no expedition with id e-404"));
    }

    /**
     * The schedule is still checked by the expedition: an activity outside its own time window is rejected
     * when it is scheduled, like any malformed plan (D21, D40).
     */
    @Test
    void answersBadRequestWhenTheScheduleDoesNotFitTheWindowOfTheActivity() throws Exception {
        final var expedition = anExpeditionInTheCoastalReef();

        scheduleIn(expedition, NIGHT_SAMPLING.replace("\"start\": \"2026-10-01T20:00:00\"", "\"start\": \"2026-10-01T10:00:00\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Night sampling must be scheduled within its time window"));
    }

    private ResultActions scheduleIn(String expeditionId, String body) throws Exception {
        return api.perform(post("/api/expeditions/{expeditionId}/activities", expeditionId)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /**
     * The scenario of F1 over HTTP: the zone, a person certified for night operations, a lamp of category
     * {@code Lighting} and an expedition that works in that zone with its special permit granted.
     */
    private String anExpeditionInTheCoastalReef() throws Exception {
        api.perform(post("/api/zones").contentType(MediaType.APPLICATION_JSON).content("""
                        {"id": "z-1", "name": "Coastal reef", "requiredPermits": []}
                        """))
                .andExpect(status().isCreated());
        api.perform(post("/api/staff").contentType(MediaType.APPLICATION_JSON).content("""
                        {"id": "p-1", "name": "Ada", "certifications": ["Night operations"]}
                        """))
                .andExpect(status().isCreated());
        api.perform(post("/api/instruments").contentType(MediaType.APPLICATION_JSON).content("""
                        {"id": "i-1", "name": "Field lamp", "category": "Lighting", "certifications": []}
                        """))
                .andExpect(status().isCreated());

        final var created = api.perform(post("/api/expeditions").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "name": "Coastal survey",
                          "period": {"start": "2026-10-01T00:00:00", "end": "2026-10-02T00:00:00"},
                          "objectives": ["Sample the reef at night"],
                          "zoneIds": ["z-1"],
                          "responsibleIds": ["p-1"],
                          "grantedPermits": ["night operations in zone z-1"],
                          "restrictions": {"maxParticipants": 10, "riskTolerance": "MEDIUM"}
                        }
                        """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(created, "$.id").toString();
    }
}
