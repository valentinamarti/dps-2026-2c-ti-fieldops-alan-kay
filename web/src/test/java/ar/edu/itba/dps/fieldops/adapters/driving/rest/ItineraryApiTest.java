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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Blocks and the itinerary over HTTP. It covers the first acceptance criterion of F2 on the API: an itinerary
 * with a parallel block inside a sequential one can be built through {@code POST /blocks} and read back as
 * such through {@code GET /itinerary}.
 *
 * <p>The whole wiring of {@code FieldOpsConfig} with the in-memory adapters and no mocks; the repositories are
 * shared beans, so each test starts with a new context (D37).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ItineraryApiTest {

    /**
     * A sampling, and after it two activities that may run at the same time: the shape F2 asks for.
     */
    private static final String PARALLEL_INSIDE_SEQUENTIAL = """
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
            activity("a-1", "Reef sampling", 8, 10, "Sample collection", "{\"sampleCount\": 3}", "[]"),
            activity("a-2", "Camp transfer", 10, 11, "Transfer", "{\"distanceKilometers\": 10}", "[]"),
            activity("a-3", "Dune survey", 10, 12, "Drone survey", "{\"areaHectares\": 20}", "[]"));

    @Autowired
    private MockMvc api;

    private static String activity(String id, String name, int fromHour, int toHour, String type,
                                   String parameters, String dependencyIds) {
        return """
                {
                  "id": "%s",
                  "name": "%s",
                  "timeWindow": {"start": "2026-10-01T00:00:00", "end": "2026-10-02T00:00:00"},
                  "scheduledPeriod": {"start": "2026-10-01T%02d:00:00", "end": "2026-10-01T%02d:00:00"},
                  "zoneId": "z-1",
                  "dependencyIds": %s,
                  "type": "%s",
                  "parameters": %s,
                  "atNight": false
                }
                """.formatted(id, name, fromHour, toHour, dependencyIds, type, parameters);
    }

    /**
     * The acceptance criterion of F2 over HTTP, on the way in: the answer is the block that was created, with
     * the parallel one inside it, and the duration each kind combines its own way (D39).
     */
    @Test
    void anItineraryCanHaveAParallelBlockInsideASequentialBlock() throws Exception {
        final var expedition = anExpeditionInTheCoastalReef();

        scheduleIn(expedition, PARALLEL_INSIDE_SEQUENTIAL)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/expeditions/%s/itinerary".formatted(expedition)))
                .andExpect(jsonPath("$.type").value("sequential"))
                .andExpect(jsonPath("$.scheduledPeriod.start").value("2026-10-01T08:00:00"))
                .andExpect(jsonPath("$.scheduledPeriod.end").value("2026-10-01T12:00:00"))
                .andExpect(jsonPath("$.estimatedDurationMinutes").value(210))
                .andExpect(jsonPath("$.children[0].type").value("activity"))
                .andExpect(jsonPath("$.children[0].activity.id").value("a-1"))
                .andExpect(jsonPath("$.children[1].type").value("parallel"))
                .andExpect(jsonPath("$.children[1].estimatedDurationMinutes").value(90))
                .andExpect(jsonPath("$.children[1].children[0].activity.name").value("Camp transfer"))
                .andExpect(jsonPath("$.children[1].children[1].activity.name").value("Dune survey"));
    }

    /**
     * And on the way out: the same tree is read back with its shape, a leaf saying which activity it is and a
     * block saying what is inside it (D54).
     */
    @Test
    void theItineraryIsReadBackWithTheShapeItWasBuiltWith() throws Exception {
        final var expedition = anExpeditionInTheCoastalReef();
        scheduleIn(expedition, PARALLEL_INSIDE_SEQUENTIAL).andExpect(status().isCreated());

        api.perform(get("/api/expeditions/{id}/itinerary", expedition))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expeditionId").value(expedition))
                .andExpect(jsonPath("$.nodes.length()").value(1))
                .andExpect(jsonPath("$.nodes[0].type").value("sequential"))
                .andExpect(jsonPath("$.nodes[0].estimatedRisk").value("LOW"))
                .andExpect(jsonPath("$.nodes[0].children.length()").value(2))
                .andExpect(jsonPath("$.nodes[0].children[0].activity.scheduledPeriod.start").value("2026-10-01T08:00:00"))
                .andExpect(jsonPath("$.nodes[0].children[0].activity.requiredSupplies[0].category").value("sample container"))
                .andExpect(jsonPath("$.nodes[0].children[1].type").value("parallel"))
                .andExpect(jsonPath("$.nodes[0].children[1].children.length()").value(2))
                // A leaf has no children and a block has no activity of its own: the field is left out.
                .andExpect(jsonPath("$.nodes[0].children[0].children").doesNotExist())
                .andExpect(jsonPath("$.nodes[0].activity").doesNotExist());
    }

    /**
     * An itinerary without blocks reads as a list of leaves, because there every activity is its own root node
     * (D39): the activities go in one by one through the endpoint of F1 and come out as roots.
     */
    @Test
    void anItineraryWithoutBlocksIsReadAsAListOfActivities() throws Exception {
        final var expedition = anExpeditionInTheCoastalReef();
        scheduleActivityIn(expedition, activity("a-1", "Reef sampling", 8, 10, "Sample collection",
                "{\"sampleCount\": 3}", "[]")).andExpect(status().isCreated());
        scheduleActivityIn(expedition, activity("a-2", "Camp transfer", 10, 11, "Transfer",
                "{\"distanceKilometers\": 10}", "[]")).andExpect(status().isCreated());

        api.perform(get("/api/expeditions/{id}/itinerary", expedition))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes.length()").value(2))
                .andExpect(jsonPath("$.nodes[0].type").value("activity"))
                .andExpect(jsonPath("$.nodes[0].activity.id").value("a-1"))
                .andExpect(jsonPath("$.nodes[0].estimatedDurationMinutes").value(120))
                .andExpect(jsonPath("$.nodes[1].type").value("activity"))
                .andExpect(jsonPath("$.nodes[1].activity.id").value("a-2"))
                .andExpect(jsonPath("$.nodes[0].children").doesNotExist())
                .andExpect(jsonPath("$.nodes[1].children").doesNotExist());
    }

    @Test
    void anItineraryOfAnExpeditionWithoutAnythingScheduledIsEmpty() throws Exception {
        api.perform(get("/api/expeditions/{id}/itinerary", anExpeditionInTheCoastalReef()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes").isEmpty());
    }

    /**
     * An activity of a block can depend on an earlier one of the same block: while the tree is being built
     * neither of the two is scheduled, so the dependency is resolved among the activities of the same request
     * (D54).
     */
    @Test
    void anActivityOfABlockCanDependOnAnEarlierOneOfTheSameBlock() throws Exception {
        final var expedition = anExpeditionInTheCoastalReef();

        scheduleIn(expedition, """
                {
                  "type": "sequential",
                  "children": [
                    {"activity": %s},
                    {"activity": %s}
                  ]
                }
                """.formatted(
                        activity("a-1", "Reef sampling", 8, 10, "Sample collection", "{\"sampleCount\": 3}", "[]"),
                        activity("a-2", "Camp transfer", 10, 11, "Transfer", "{\"distanceKilometers\": 10}", "[\"a-1\"]")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.children[1].activity.dependencyIds[0]").value("a-1"));
    }

    /**
     * Declared the other way round it does not exist yet, so it reads like any dependency nobody scheduled.
     */
    @Test
    void answersNotFoundWhenAnActivityDependsOnALaterOneOfTheSameBlock() throws Exception {
        final var expedition = anExpeditionInTheCoastalReef();

        scheduleIn(expedition, """
                {
                  "type": "sequential",
                  "children": [
                    {"activity": %s},
                    {"activity": %s}
                  ]
                }
                """.formatted(
                        activity("a-1", "Reef sampling", 8, 10, "Sample collection", "{\"sampleCount\": 3}", "[\"a-2\"]"),
                        activity("a-2", "Camp transfer", 10, 11, "Transfer", "{\"distanceKilometers\": 10}", "[]")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no scheduled activity with id a-2"));
    }

    @Test
    void answersBadRequestForABlockWithNothingInside() throws Exception {
        final var expedition = anExpeditionInTheCoastalReef();

        scheduleIn(expedition, """
                {"type": "parallel", "children": []}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("a block needs at least one activity or block inside"));
    }

    @Test
    void answersBadRequestForAKindOfBlockThatDoesNotExist() throws Exception {
        final var expedition = anExpeditionInTheCoastalReef();

        scheduleIn(expedition, PARALLEL_INSIDE_SEQUENTIAL.replace("\"sequential\"", "\"spiral\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "there is no kind of block named spiral: a block is sequential or parallel"));
    }

    @Test
    void answersBadRequestForASequentialBlockWhoseChildrenOverlap() throws Exception {
        final var expedition = anExpeditionInTheCoastalReef();

        scheduleIn(expedition, PARALLEL_INSIDE_SEQUENTIAL.replace("\"2026-10-01T10:00:00\", \"end\": \"2026-10-01T11:00:00\"",
                "\"2026-10-01T09:00:00\", \"end\": \"2026-10-01T11:00:00\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "each part of a sequential block must start once the previous one has ended"));
    }

    /**
     * Two activities of different branches of the same parallel block cannot depend on each other, because the
     * block declares that they may run at the same time (D40).
     */
    @Test
    void answersBadRequestForADependencyBetweenBranchesOfTheSameParallelBlock() throws Exception {
        final var expedition = anExpeditionInTheCoastalReef();

        scheduleIn(expedition, """
                {
                  "type": "parallel",
                  "children": [
                    {"activity": %s},
                    {"activity": %s}
                  ]
                }
                """.formatted(
                        activity("a-1", "Reef sampling", 8, 10, "Sample collection", "{\"sampleCount\": 3}", "[]"),
                        activity("a-2", "Camp transfer", 10, 11, "Transfer", "{\"distanceKilometers\": 10}", "[\"a-1\"]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(Matchers.endsWith("which runs in parallel with it in the same block")));
    }

    @Test
    void answersNotFoundForAnExpeditionThatDoesNotExist() throws Exception {
        scheduleIn("e-404", PARALLEL_INSIDE_SEQUENTIAL)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no expedition with id e-404"));

        api.perform(get("/api/expeditions/{id}/itinerary", "e-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no expedition with id e-404"));
    }

    private ResultActions scheduleIn(String expeditionId, String body) throws Exception {
        return api.perform(post("/api/expeditions/{expeditionId}/blocks", expeditionId)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions scheduleActivityIn(String expeditionId, String body) throws Exception {
        return api.perform(post("/api/expeditions/{expeditionId}/activities", expeditionId)
                .contentType(MediaType.APPLICATION_JSON).content(body));
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
