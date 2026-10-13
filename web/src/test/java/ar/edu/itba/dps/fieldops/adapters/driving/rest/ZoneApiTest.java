package ar.edu.itba.dps.fieldops.adapters.driving.rest;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ZoneApiTest {
    private static final String COASTAL_REEF = """
            {"id": "z-1", "name": "Coastal reef", "requiredPermits": ["Protected area"]}
            """;

    @Autowired
    private MockMvc api;

    @Test
    void registersAZoneAndSaysWhereToFindIt() throws Exception {
        registerCoastalReef()
                .andExpect(header().string("Location", "/api/zones/z-1"))
                .andExpect(jsonPath("$.id").value("z-1"))
                .andExpect(jsonPath("$.name").value("Coastal reef"))
                .andExpect(jsonPath("$.requiredPermits[0]").value("Protected area"));

        api.perform(get("/api/zones/{id}", "z-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Coastal reef"));
    }

    @Test
    void listsTheZonesThatWereRegistered() throws Exception {
        registerCoastalReef();
        api.perform(post("/api/zones").contentType(MediaType.APPLICATION_JSON)
                        .content(COASTAL_REEF.replace("z-1", "z-2")))
                .andExpect(status().isCreated());

        api.perform(get("/api/zones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("z-1"));
    }

    @Test
    void rejectsASecondZoneWithTheSameId() throws Exception {
        registerCoastalReef();

        api.perform(post("/api/zones").contentType(MediaType.APPLICATION_JSON).content(COASTAL_REEF))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("A zone with id z-1 is already registered"));
    }

    @Test
    void answersNotFoundForAZoneThatIsNotRegistered() throws Exception {
        api.perform(get("/api/zones/{id}", "z-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no zone with id z-404"));
    }

    @Test
    void answersBadRequestWhenTheBodyDoesNotDescribeAZone() throws Exception {
        api.perform(post("/api/zones").contentType(MediaType.APPLICATION_JSON).content("""
                        {"id": "z-1", "requiredPermits": []}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("name is required"));
    }

    private ResultActions registerCoastalReef() throws Exception {
        return api.perform(post("/api/zones").contentType(MediaType.APPLICATION_JSON).content(COASTAL_REEF))
                .andExpect(status().isCreated());
    }
}
