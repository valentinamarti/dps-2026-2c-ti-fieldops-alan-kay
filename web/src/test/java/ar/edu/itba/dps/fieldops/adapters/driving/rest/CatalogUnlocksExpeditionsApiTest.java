package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * What the catalog and the zones unlock: before this PR an expedition could only be created by a test that
 * registered its zone and its responsible in Java, because the API had no way of registering either. This
 * walks the whole path over HTTP, with nothing loaded beforehand.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CatalogUnlocksExpeditionsApiTest {

    @Autowired
    private MockMvc api;

    @Test
    void createsAnExpeditionReferencingAZoneAndAPersonRegisteredOverTheApi() throws Exception {
        api.perform(post("/api/zones").contentType(MediaType.APPLICATION_JSON).content("""
                        {"id": "z-1", "name": "Coastal reef", "requiredPermits": []}
                        """))
                .andExpect(status().isCreated());
        api.perform(post("/api/staff").contentType(MediaType.APPLICATION_JSON).content("""
                        {"id": "p-1", "name": "Ada", "certifications": ["Diving"]}
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
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();

        api.perform(get("/api/expeditions/{id}", JsonPath.read(created, "$.id").toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zoneIds[0]").value("z-1"))
                .andExpect(jsonPath("$.responsibleIds[0]").value("p-1"));
    }

    /**
     * The zone and the person have to be registered first: the expedition references them by id and the use
     * case resolves them (D38), so an unregistered id is a 404 and not an empty expedition.
     */
    @Test
    void refusesToCreateAnExpeditionBeforeTheCatalogAndTheZonesAreRegistered() throws Exception {
        api.perform(post("/api/expeditions").contentType(MediaType.APPLICATION_JSON).content("""
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
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no zone with id z-1"));
    }
}
