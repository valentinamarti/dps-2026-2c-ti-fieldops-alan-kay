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
class AssignmentApiTest {
    @Autowired
    private MockMvc api;

    @Test
    void assignsStaffEquipmentAndASupplyAndTheyShowInTheItineraryAfterwards() throws Exception {
        final var expedition = anExpeditionWithACandidateForEachRequirement();
        scheduleSampleCollection(expedition, "a-1", 2).andExpect(status().isCreated());

        assign(expedition, "a-1", "staff", "p-2").andExpect(status().isNoContent());
        assign(expedition, "a-1", "equipment", "i-1").andExpect(status().isNoContent());
        assign(expedition, "a-1", "supplies", "s-1").andExpect(status().isNoContent());

        api.perform(get("/api/expeditions/{id}/itinerary", expedition))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes[0].activity.assignedStaffIds").value(Matchers.contains("p-2")))
                .andExpect(jsonPath("$.nodes[0].activity.assignedEquipmentIds").value(Matchers.contains("i-1")))
                .andExpect(jsonPath("$.nodes[0].activity.assignedSupplyIds").value(Matchers.contains("s-1")));
    }

    @Test
    void candidatesReflectWhatTheCatalogCanOfferForEachRequirementOfTheActivity() throws Exception {
        final var expedition = anExpeditionWithACandidateForEachRequirement();
        scheduleSampleCollection(expedition, "a-1", 2).andExpect(status().isCreated());

        api.perform(get("/api/expeditions/{expeditionId}/activities/{activityId}/candidates", expedition, "a-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.staff[0].requirement.headcount").value(1))
                .andExpect(jsonPath("$.staff[0].requirement.certifications").isEmpty())
                .andExpect(jsonPath("$.staff[0].candidates[*].id").value(Matchers.hasItem("p-2")))
                .andExpect(jsonPath("$.equipment[0].requirement.category").value("cooler"))
                .andExpect(jsonPath("$.equipment[0].requirement.count").value(1))
                .andExpect(jsonPath("$.equipment[0].vehicles").isEmpty())
                .andExpect(jsonPath("$.equipment[0].instruments[*].id").value(Matchers.hasItem("i-1")))
                .andExpect(jsonPath("$.supplies[0].requirement.category").value("sample container"))
                .andExpect(jsonPath("$.supplies[0].requirement.quantity.amount").value("2"))
                .andExpect(jsonPath("$.supplies[0].candidates[*].id").value(Matchers.hasItem("s-1")));
    }

    @Test
    void assigningEquipmentOfTheWrongCategorySucceedsAndTheMismatchShowsUpOnlyAtValidation() throws Exception {
        final var expedition = anExpeditionWithACandidateForEachRequirement();
        register("/api/instruments", """
                {"id": "i-2", "name": "GPS unit", "category": "Navigation", "certifications": []}
                """).andExpect(status().isCreated());
        scheduleSampleCollection(expedition, "a-1", 2).andExpect(status().isCreated());

        assign(expedition, "a-1", "equipment", "i-2").andExpect(status().isNoContent());

        api.perform(get("/api/expeditions/{id}/validation", expedition))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.criticals[?(@.rule == 'missing-resources' && @.subject.activityId == 'a-1')].message")
                        .value(Matchers.hasItem("Sample collection a-1 has 0 of the 1 cooler it needs")));
    }

    @Test
    void answersNotFoundWhenAssigningOrReadingCandidatesOfAnExpeditionThatDoesNotExist() throws Exception {
        assign("e-404", "a-1", "staff", "p-1")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no expedition with id e-404"));
        assign("e-404", "a-1", "equipment", "i-1")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no expedition with id e-404"));
        assign("e-404", "a-1", "supplies", "s-1")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no expedition with id e-404"));

        api.perform(get("/api/expeditions/{expeditionId}/activities/{activityId}/candidates", "e-404", "a-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no expedition with id e-404"));
    }

    @Test
    void answersNotFoundWhenTheActivityWasNeverScheduled() throws Exception {
        final var expedition = anExpeditionWithACandidateForEachRequirement();

        assign(expedition, "a-404", "staff", "p-2")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no scheduled activity with id a-404"));

        api.perform(get("/api/expeditions/{expeditionId}/activities/{activityId}/candidates", expedition, "a-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no scheduled activity with id a-404"));
    }

    @Test
    void answersNotFoundWhenTheResourceDoesNotExistInTheCatalog() throws Exception {
        final var expedition = anExpeditionWithACandidateForEachRequirement();
        scheduleSampleCollection(expedition, "a-1", 2).andExpect(status().isCreated());

        assign(expedition, "a-1", "staff", "p-404")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no person with id p-404"));
        assign(expedition, "a-1", "equipment", "i-404")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no equipment with id i-404"));
        assign(expedition, "a-1", "supplies", "s-404")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no supply with id s-404"));
    }

    private ResultActions assign(String expeditionId, String activityId, String kind, String resourceId) throws Exception {
        return api.perform(post("/api/expeditions/{expeditionId}/activities/{activityId}/{kind}",
                        expeditionId, activityId, kind)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"resourceId": "%s"}
                        """.formatted(resourceId)));
    }

    private ResultActions scheduleSampleCollection(String expeditionId, String activityId, int sampleCount) throws Exception {
        return api.perform(post("/api/expeditions/{expeditionId}/activities", expeditionId)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "id": "%s",
                          "name": "Sample collection %s",
                          "timeWindow": {"start": "2026-10-01T00:00:00", "end": "2026-10-02T00:00:00"},
                          "scheduledPeriod": {"start": "2026-10-01T08:00:00", "end": "2026-10-01T10:00:00"},
                          "zoneId": "z-1",
                          "dependencyIds": [],
                          "type": "Sample collection",
                          "parameters": {"sampleCount": %d},
                          "atNight": false
                        }
                        """.formatted(activityId, activityId, sampleCount)));
    }

    private ResultActions register(String path, String body) throws Exception {
        return api.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String anExpeditionWithACandidateForEachRequirement() throws Exception {
        register("/api/zones", """
                {"id": "z-1", "name": "Coastal reef", "requiredPermits": []}
                """).andExpect(status().isCreated());
        register("/api/staff", """
                {"id": "p-1", "name": "Ada", "certifications": []}
                """).andExpect(status().isCreated());
        register("/api/staff", """
                {"id": "p-2", "name": "Lee", "certifications": []}
                """).andExpect(status().isCreated());
        register("/api/instruments", """
                {"id": "i-1", "name": "Field cooler", "category": "Cooler", "certifications": []}
                """).andExpect(status().isCreated());
        register("/api/supplies", """
                {"id": "s-1", "name": "Sample containers", "category": "Sample container",
                 "initialStock": {"amount": "10", "unit": "UNITS"}}
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
        return JsonPath.read(created, "$.id").toString();
    }
}
