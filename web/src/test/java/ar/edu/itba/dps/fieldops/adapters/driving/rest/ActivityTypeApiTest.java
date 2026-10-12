package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The endpoint that tells a client what it can ask for when it schedules an activity. It does not need a new
 * context per test, unlike the ones that register things: the registry of types is decided by the domain and
 * nothing over HTTP changes it (D44).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ActivityTypeApiTest {

    @Autowired
    private MockMvc api;

    @Test
    void listsTheSixTypesWithTheParameterEachOneNeeds() throws Exception {
        api.perform(get("/api/activity-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[*].name").value(Matchers.contains(
                        "sample collection", "diving", "drone survey", "wildlife observation",
                        "water quality measurement", "transfer")))
                .andExpect(jsonPath("$[0].parameterNames").value("sampleCount"))
                .andExpect(jsonPath("$[1].parameterNames").value("maxDepthMeters"))
                .andExpect(jsonPath("$[2].parameterNames").value("areaHectares"))
                .andExpect(jsonPath("$[3].parameterNames").value("observationPoints"))
                .andExpect(jsonPath("$[4].parameterNames").value("stationCount"))
                .andExpect(jsonPath("$[5].parameterNames").value("distanceKilometers"));
    }

    /**
     * The names come normalized, so a type asked for as {@code Sample collection} is published as the value
     * the domain compares (D34), and the names of the parameters do not: they are identifiers and travel as
     * they have to be sent.
     */
    @Test
    void publishesTheNamesTheDomainComparesAndTheParametersAsTheyAreSent() throws Exception {
        api.perform(get("/api/activity-types"))
                .andExpect(jsonPath("$[?(@.name == 'water quality measurement')].parameterNames[0]")
                        .value("stationCount"));
    }
}
