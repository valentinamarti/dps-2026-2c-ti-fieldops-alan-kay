package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The expedition resource over the whole stack: the real wiring of {@code FieldOpsConfig} with the in-memory
 * adapters, no mocks. The repositories and the catalog are shared beans, so each test starts with a new
 * context, like the other integration tests of this module (D37).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ExpeditionApiTest {

    private static final String COASTAL_SURVEY = """
            {
              "name": "Coastal survey",
              "period": {"start": "2026-10-01T00:00:00", "end": "2026-10-02T00:00:00"},
              "objectives": ["Map the coastal reef"],
              "zoneIds": ["z-1"],
              "responsibleIds": ["p-1"],
              "grantedPermits": [],
              "restrictions": {"maxParticipants": 10, "riskTolerance": "MEDIUM"}
            }
            """;

    @Autowired
    private MockMvc api;

    @Autowired
    private ZoneRepository zones;

    @Autowired
    private ResourceCatalog catalog;

    @BeforeEach
    void registerWhatAnExpeditionReferences() {
        zones.add(zone("z-1"));
        catalog.addStaff(person("p-1"));
    }

    @Test
    void createsAnExpeditionInDraftAndSaysWhereToFindIt() throws Exception {
        final var created = api.perform(post("/api/expeditions").contentType(MediaType.APPLICATION_JSON).content(COASTAL_SURVEY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/expeditions/")))
                .andExpect(jsonPath("$.name").value("Coastal survey"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.zoneIds[0]").value("z-1"))
                .andExpect(jsonPath("$.responsibleIds[0]").value("p-1"))
                .andExpect(jsonPath("$.restrictions.maxParticipants").value(10))
                .andExpect(jsonPath("$.restrictions.riskTolerance").value("MEDIUM"))
                .andExpect(jsonPath("$.period.start").value("2026-10-01T00:00:00"))
                .andReturn().getResponse().getContentAsString();

        api.perform(get("/api/expeditions/{id}", JsonPath.read(created, "$.id").toString()))
                .andExpect(status().isOk());
    }

    /**
     * The tree has its own endpoint and how it is serialized is still open, so it is not part of this response.
     */
    @Test
    void leavesTheItineraryOutOfTheExpeditionResponse() throws Exception {
        api.perform(post("/api/expeditions").contentType(MediaType.APPLICATION_JSON).content(COASTAL_SURVEY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itinerary").doesNotExist());
    }

    @Test
    void listsTheExpeditionsThatWereCreated() throws Exception {
        createCoastalSurvey();
        createCoastalSurvey();

        api.perform(get("/api/expeditions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Coastal survey"));
    }

    @Test
    void getsAnExpeditionByItsId() throws Exception {
        final var id = createCoastalSurvey();

        api.perform(get("/api/expeditions/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.objectives[0]").value("Map the coastal reef"));
    }

    @Test
    void answersNotFoundForAnExpeditionThatDoesNotExist() throws Exception {
        api.perform(get("/api/expeditions/{id}", "e-404"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("There is no expedition with id e-404"));
    }

    /**
     * The adapter does not validate; what comes back is the message of the domain, as a 400.
     */
    @Test
    void answersBadRequestWhenTheBodyDoesNotDescribeAnExpedition() throws Exception {
        api.perform(post("/api/expeditions").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "name": "Coastal survey",
                          "period": {"start": "2026-10-01T00:00:00", "end": "2026-10-02T00:00:00"},
                          "objectives": ["Map the coastal reef"],
                          "zoneIds": [],
                          "responsibleIds": ["p-1"],
                          "restrictions": {"maxParticipants": 10, "riskTolerance": "MEDIUM"}
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("zones cannot be empty"));
    }

    @Test
    void answersNotFoundWhenTheExpeditionReferencesAZoneThatIsNotRegistered() throws Exception {
        api.perform(post("/api/expeditions").contentType(MediaType.APPLICATION_JSON)
                        .content(COASTAL_SURVEY.replace("z-1", "z-404")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no zone with id z-404"));
    }

    private String createCoastalSurvey() throws Exception {
        final var body = api.perform(post("/api/expeditions").contentType(MediaType.APPLICATION_JSON).content(COASTAL_SURVEY))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }
}
