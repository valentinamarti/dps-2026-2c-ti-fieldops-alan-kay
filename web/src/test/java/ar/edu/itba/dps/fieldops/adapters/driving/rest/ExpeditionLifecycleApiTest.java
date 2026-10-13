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

/**
 * The life of an expedition over HTTP, end to end: every transition of D33, approving with a warning accepted
 * the same way the demo script does (copying it out of {@code GET .../validation}), tracking an activity while
 * the expedition executes it, and the report that closes the story (D49, D57). The whole wiring of
 * {@code FieldOpsConfig} with the in-memory adapters, no mocks (D37).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ExpeditionLifecycleApiTest {

    @Autowired
    private MockMvc api;

    /**
     * Draft -> in review -> approved -> in execution -> suspended -> in execution again -> finished, with the
     * one scheduled activity started, observed and finished with a result in between. The report at the end
     * reflects what actually happened: one of one activities finished, and the sample container it consumed.
     */
    @Test
    void theFullLifecycleMovesThroughEveryStateWithTheActivityTrackedAlongTheWay() throws Exception {
        final var expedition = anExpeditionWithASampleCollectionFullyResourced();

        transition(expedition, "review").andExpect(status().isNoContent());
        assertStatus(expedition, "IN_REVIEW");

        approve(expedition, "{}").andExpect(status().isNoContent());
        assertStatus(expedition, "APPROVED");

        transition(expedition, "execution").andExpect(status().isNoContent());
        assertStatus(expedition, "IN_EXECUTION");

        api.perform(post("/api/expeditions/{expeditionId}/activities/{activityId}/start", expedition, "a-1")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"at": "2026-10-01T08:00:00"}
                                """))
                .andExpect(status().isNoContent());

        api.perform(post("/api/expeditions/{expeditionId}/activities/{activityId}/observations", expedition, "a-1")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"note": "Calm seas, on schedule", "recordedAt": "2026-10-01T08:30:00"}
                                """))
                .andExpect(status().isNoContent());

        transition(expedition, "suspension").andExpect(status().isNoContent());
        assertStatus(expedition, "SUSPENDED");

        transition(expedition, "resumption").andExpect(status().isNoContent());
        assertStatus(expedition, "IN_EXECUTION");

        api.perform(post("/api/expeditions/{expeditionId}/activities/{activityId}/completion", expedition, "a-1")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"at": "2026-10-01T09:00:00", "result": "Collected 1 sample"}
                                """))
                .andExpect(status().isNoContent());

        transition(expedition, "completion").andExpect(status().isNoContent());
        assertStatus(expedition, "FINISHED");

        api.perform(get("/api/expeditions/{id}/report", expedition))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.scheduledActivities").value(1))
                .andExpect(jsonPath("$.finishedActivities").value(1))
                .andExpect(jsonPath("$.estimate.totalDurationMinutes").value(80))
                .andExpect(jsonPath("$.actualConsumption[0].category").value("sample container"))
                .andExpect(jsonPath("$.actualConsumption[0].quantity.amount").value("1"))
                .andExpect(jsonPath("$.incidents").isEmpty())
                .andExpect(jsonPath("$.acceptedWarnings").isEmpty());
    }

    /**
     * The symmetry the demo script relies on: the body of the approval is built by copying the rule and the
     * subject straight out of a warning {@code GET .../validation} returned, adding only who accepts it and
     * why. The report afterwards shows the same warning back, reusing {@code ValidationFindingDto} (D57).
     */
    @Test
    void approvalAcceptsAWarningCopiedFromTheValidationResponseAndTheReportShowsItAccepted() throws Exception {
        final var expedition = anExpeditionWithARiskyDroneSurveyFullyResourced();

        transition(expedition, "review").andExpect(status().isNoContent());

        final var validation = api.perform(get("/api/expeditions/{id}/validation", expedition))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canBeApproved").value(true))
                .andExpect(jsonPath("$.warnings[0].rule").value("risk-tolerance"))
                .andReturn().getResponse().getContentAsString();

        final String rule = JsonPath.read(validation, "$.warnings[0].rule");
        final String activityId = readOrNull(validation, "$.warnings[0].subject.activityId");
        final String resourceId = readOrNull(validation, "$.warnings[0].subject.resourceId");

        approve(expedition, """
                        {
                          "acceptedWarnings": [
                            {
                              "rule": "%s",
                              "subject": {"activityId": %s, "resourceId": %s},
                              "responsibleId": "p-1",
                              "justification": "The area has no obstacles close to the line of sight limit"
                            }
                          ]
                        }
                        """.formatted(rule, jsonOrNull(activityId), jsonOrNull(resourceId)))
                .andExpect(status().isNoContent());
        assertStatus(expedition, "APPROVED");

        api.perform(get("/api/expeditions/{id}/report", expedition))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceptedWarnings[0].warning.rule").value("risk-tolerance"))
                .andExpect(jsonPath("$.acceptedWarnings[0].warning.subject.activityId").value("a-1"))
                .andExpect(jsonPath("$.acceptedWarnings[0].responsibleId").value("p-1"))
                .andExpect(jsonPath("$.acceptedWarnings[0].justification")
                        .value("The area has no obstacles close to the line of sight limit"));
    }

    /**
     * The other direction of the same rule (D22): a critical is not something a responsible can accept, so
     * approving with one still open fails, and the body says which one (D57; it is the message of
     * {@code ExpeditionNotApprovableException}, which already lists every critical it blocked on).
     */
    @Test
    void approvingWithACriticalStillOpenFailsAndTheBodyListsIt() throws Exception {
        final var expedition = anExpeditionWithASampleCollectionMissingItsResources();
        transition(expedition, "review").andExpect(status().isNoContent());

        approve(expedition, "{}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("cannot be approved")))
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("has 0 of the 1 staff members it needs")))
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("has 0 of the 1 cooler it needs")));
    }

    /**
     * Accepting a warning for a rule and a subject the expedition does not currently raise is the same "not
     * found" the Fase 1b left for every other lookup (D49): the pair identifies the finding (D32), and no
     * finding with that identity exists right now.
     */
    @Test
    void acceptingAWarningTheExpeditionDoesNotRaiseAnswersNotFound() throws Exception {
        final var expedition = anExpeditionWithASampleCollectionMissingItsResources();
        transition(expedition, "review").andExpect(status().isNoContent());

        approve(expedition, """
                        {
                          "acceptedWarnings": [
                            {
                              "rule": "risk-tolerance",
                              "subject": {"activityId": "a-1", "resourceId": null},
                              "responsibleId": "p-1",
                              "justification": "Not actually raised"
                            }
                          ]
                        }
                        """)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("There is no warning")));
    }

    /**
     * An illegal transition answers 409, with the domain's own explanation of which states it allows (D20,
     * D33): a draft expedition has not been through review or approval yet.
     */
    @Test
    void anIllegalTransitionAnswersConflict() throws Exception {
        final var expedition = anExpeditionWithASampleCollectionFullyResourced();

        transition(expedition, "execution")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("An expedition cannot go from DRAFT to IN_EXECUTION"));
    }

    @Test
    void answersNotFoundForLifecycleAndTrackingOperationsOnAnExpeditionThatDoesNotExist() throws Exception {
        for (final var subResource : new String[] {"review", "draft", "approval", "execution", "suspension", "resumption", "completion"}) {
            api.perform(post("/api/expeditions/{expeditionId}/{sub}", "e-404", subResource)
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("There is no expedition with id e-404"));
        }
        api.perform(get("/api/expeditions/{id}/report", "e-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no expedition with id e-404"));
        api.perform(post("/api/expeditions/{expeditionId}/activities/{activityId}/start", "e-404", "a-1")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"at": "2026-10-01T08:00:00"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no expedition with id e-404"));
    }

    private ResultActions transition(String expeditionId, String subResource) throws Exception {
        return api.perform(post("/api/expeditions/{expeditionId}/{sub}", expeditionId, subResource)
                .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions approve(String expeditionId, String body) throws Exception {
        return api.perform(post("/api/expeditions/{expeditionId}/approval", expeditionId)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void assertStatus(String expeditionId, String expected) throws Exception {
        api.perform(get("/api/expeditions/{id}", expeditionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(expected));
    }

    private String readOrNull(String json, String path) {
        try {
            return JsonPath.read(json, path);
        } catch (com.jayway.jsonpath.PathNotFoundException notFound) {
            return null;
        }
    }

    private static String jsonOrNull(String value) {
        return value == null ? "null" : "\"%s\"".formatted(value);
    }

    private ResultActions register(String path, String body) throws Exception {
        return api.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions scheduleActivity(String expeditionId, String body) throws Exception {
        return api.perform(post("/api/expeditions/{expeditionId}/activities", expeditionId)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions assign(String expeditionId, String activityId, String kind, String resourceId) throws Exception {
        return api.perform(post("/api/expeditions/{expeditionId}/activities/{activityId}/{kind}",
                        expeditionId, activityId, kind)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"resourceId": "%s"}
                        """.formatted(resourceId)));
    }

    private String createExpedition(String riskTolerance) throws Exception {
        final var created = api.perform(post("/api/expeditions").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "name": "Coastal survey",
                          "period": {"start": "2026-10-01T00:00:00", "end": "2026-10-02T00:00:00"},
                          "objectives": ["Map the coastal reef"],
                          "zoneIds": ["z-1"],
                          "responsibleIds": ["p-1"],
                          "grantedPermits": [],
                          "restrictions": {"maxParticipants": 10, "riskTolerance": "%s"}
                        }
                        """.formatted(riskTolerance)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(created, "$.id").toString();
    }

    /**
     * A "Sample collection" with a field worker, a cooler and a sample container assigned: nothing missing,
     * low risk, so it can go all the way to approved without accepting anything.
     */
    private String anExpeditionWithASampleCollectionFullyResourced() throws Exception {
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
                 "initialStock": {"amount": "5", "unit": "UNITS"}}
                """).andExpect(status().isCreated());

        final var expedition = createExpedition("MEDIUM");
        scheduleActivity(expedition, """
                        {
                          "id": "a-1",
                          "name": "Sample collection a-1",
                          "timeWindow": {"start": "2026-10-01T00:00:00", "end": "2026-10-02T00:00:00"},
                          "scheduledPeriod": {"start": "2026-10-01T08:00:00", "end": "2026-10-01T09:00:00"},
                          "zoneId": "z-1",
                          "dependencyIds": [],
                          "type": "Sample collection",
                          "parameters": {"sampleCount": 1},
                          "atNight": false
                        }
                        """)
                .andExpect(status().isCreated());

        assign(expedition, "a-1", "staff", "p-2").andExpect(status().isNoContent());
        assign(expedition, "a-1", "equipment", "i-1").andExpect(status().isNoContent());
        assign(expedition, "a-1", "supplies", "s-1").andExpect(status().isNoContent());
        return expedition;
    }

    /**
     * The same "Sample collection" as above, scheduled but with nothing assigned: missing one field worker
     * and one cooler, both criticals.
     */
    private String anExpeditionWithASampleCollectionMissingItsResources() throws Exception {
        register("/api/zones", """
                {"id": "z-1", "name": "Coastal reef", "requiredPermits": []}
                """).andExpect(status().isCreated());
        register("/api/staff", """
                {"id": "p-1", "name": "Ada", "certifications": []}
                """).andExpect(status().isCreated());

        final var expedition = createExpedition("MEDIUM");
        scheduleActivity(expedition, """
                        {
                          "id": "a-1",
                          "name": "Sample collection a-1",
                          "timeWindow": {"start": "2026-10-01T00:00:00", "end": "2026-10-02T00:00:00"},
                          "scheduledPeriod": {"start": "2026-10-01T08:00:00", "end": "2026-10-01T09:00:00"},
                          "zoneId": "z-1",
                          "dependencyIds": [],
                          "type": "Sample collection",
                          "parameters": {"sampleCount": 1},
                          "atNight": false
                        }
                        """)
                .andExpect(status().isCreated());
        return expedition;
    }

    /**
     * A "Drone survey" past the line-of-sight limit (medium risk), with a certified pilot and a drone
     * assigned: nothing missing, so the only thing standing between it and approval is the risk warning
     * ({@code RiskToleranceValidation}), because the expedition's tolerance is low.
     */
    private String anExpeditionWithARiskyDroneSurveyFullyResourced() throws Exception {
        register("/api/zones", """
                {"id": "z-1", "name": "Coastal reef", "requiredPermits": []}
                """).andExpect(status().isCreated());
        register("/api/staff", """
                {"id": "p-1", "name": "Ada", "certifications": []}
                """).andExpect(status().isCreated());
        register("/api/staff", """
                {"id": "p-2", "name": "Lee", "certifications": ["Drone pilot"]}
                """).andExpect(status().isCreated());
        register("/api/instruments", """
                {"id": "i-1", "name": "Survey drone", "category": "Drone", "certifications": []}
                """).andExpect(status().isCreated());

        final var expedition = createExpedition("LOW");
        scheduleActivity(expedition, """
                        {
                          "id": "a-1",
                          "name": "Drone survey a-1",
                          "timeWindow": {"start": "2026-10-01T00:00:00", "end": "2026-10-02T00:00:00"},
                          "scheduledPeriod": {"start": "2026-10-01T08:00:00", "end": "2026-10-01T09:00:00"},
                          "zoneId": "z-1",
                          "dependencyIds": [],
                          "type": "Drone survey",
                          "parameters": {"areaHectares": 60},
                          "atNight": false
                        }
                        """)
                .andExpect(status().isCreated());

        assign(expedition, "a-1", "staff", "p-2").andExpect(status().isNoContent());
        assign(expedition, "a-1", "equipment", "i-1").andExpect(status().isNoContent());
        return expedition;
    }
}
