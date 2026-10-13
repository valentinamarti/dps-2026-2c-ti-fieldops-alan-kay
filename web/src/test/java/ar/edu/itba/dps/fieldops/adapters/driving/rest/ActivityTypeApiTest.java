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

    @Test
    void publishesTheNamesTheDomainComparesAndTheParametersAsTheyAreSent() throws Exception {
        api.perform(get("/api/activity-types"))
                .andExpect(jsonPath("$[?(@.name == 'water quality measurement')].parameterNames[0]")
                        .value("stationCount"));
    }
}
