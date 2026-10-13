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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReplanningApiTest {
    @Autowired
    private MockMvc api;

    @Test
    void registeringAnIncidentAnswersWithTheProposalItGeneratedAndItsOptions() throws Exception {
        final var expedition = anExpeditionInExecutionWithTheSampleCollectionStarted();

        final var registered = registerIncident(expedition, theCoolerFailedTakingLeeOutFor("120"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalExpeditionId").value(expedition))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.decision").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.supersededByProposalId").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.incident.id").value(expedition + "/incidents/1"))
                .andExpect(jsonPath("$.incident.activityId").value("a-1"))
                .andExpect(jsonPath("$.incident.description").value("The cooler stopped working and Lee went back to base"))
                .andExpect(jsonPath("$.incident.occurredAt").value("2026-10-01T13:30:00"))
                .andExpect(jsonPath("$.incident.resourceId").value("p-2"))
                .andExpect(jsonPath("$.incident.expectedDelayMinutes").value(120))
                .andExpect(jsonPath("$.options[*].number").value(Matchers.contains(1, 2, 3, 4)))
                .andExpect(jsonPath("$.options[*].summary").value(Matchers.contains(
                        "Run Sample collection a-1 2 h later",
                        "Replace p-2 with p-1 in Sample collection a-1",
                        "Replace p-2 with p-3 in Sample collection a-1",
                        "Cancel Sample collection a-1")))
                .andReturn().getResponse();

        final String proposal = JsonPath.read(registered.getContentAsString(), "$.id");
        assertEquals("/api/replan-proposals/" + proposal, registered.getHeader("Location"));
    }

    @Test
    void theResponsibleAcceptsAnOptionAndGetsTheAlternativeAsADraftThatPointsAtTheOriginal() throws Exception {
        final var original = anExpeditionInExecutionWithTheSampleCollectionStarted();
        final var proposal = aProposalFor(original, theCoolerFailedTakingLeeOutFor("120"));

        finishTheStartedActivity(original);

        final var accepted = accept(proposal, 1, "p-1", "Two hours is enough to get a spare cooler on site")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.name").value("Coastal survey"))
                .andExpect(jsonPath("$.replacedExpeditionId").value(original))
                .andExpect(jsonPath("$.replacementId").value(Matchers.nullValue()))
                .andReturn().getResponse();

        final String alternative = JsonPath.read(accepted.getContentAsString(), "$.id");
        assertEquals("/api/expeditions/" + alternative, accepted.getHeader("Location"));
        assertStatus(original, "SUSPENDED");

        api.perform(get("/api/expeditions/{id}/itinerary", alternative))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes[0].activity.scheduledPeriod.start").value("2026-10-01T15:00:00"))
                .andExpect(jsonPath("$.nodes[0].activity.scheduledPeriod.end").value("2026-10-01T17:00:00"))
                .andExpect(jsonPath("$.nodes[0].activity.assignedStaffIds").value(Matchers.contains("p-2")));
    }

    @Test
    void theResponsibleRejectsTheProposalAndTheExpeditionKeepsRunningWithThePlanItHad() throws Exception {
        final var expedition = anExpeditionInExecutionWithTheSampleCollectionStarted();
        final var proposal = aProposalFor(expedition, theCoolerFailedTakingLeeOutFor("120"));

        reject(proposal, "p-1", "Lee is coming back with a spare cooler, the plan holds")
                .andExpect(status().isNoContent());

        assertStatus(expedition, "IN_EXECUTION");
        api.perform(get("/api/expeditions/{id}", expedition))
                .andExpect(jsonPath("$.replacementId").value(Matchers.nullValue()));
        api.perform(get("/api/replan-proposals/{id}", proposal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.decision.responsibleId").value("p-1"))
                .andExpect(jsonPath("$.decision.justification")
                        .value("Lee is coming back with a spare cooler, the plan holds"))
                .andExpect(jsonPath("$.decision.decidedAt").value("2026-10-01T13:45:00"))
                .andExpect(jsonPath("$.decision.chosenOptionNumber").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.decision.alternativeExpeditionId").value(Matchers.nullValue()));
    }

    @Test
    void afterTheDecisionTheOriginalPlanTheIncidentAndTheDecisionAreAllQueryable() throws Exception {
        final var original = anExpeditionInExecutionWithTheSampleCollectionStarted();
        final var proposal = aProposalFor(original, theCoolerFailedTakingLeeOutFor("120"));
        finishTheStartedActivity(original);
        final var alternative = JsonPath.<String>read(accept(proposal, 1, "p-1", "A spare cooler is on its way")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");

        transition(alternative, "review").andExpect(status().isNoContent());
        approve(alternative).andExpect(status().isNoContent());

        api.perform(get("/api/replan-proposals/{id}", proposal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.originalExpeditionId").value(original))
                .andExpect(jsonPath("$.incident.id").value(original + "/incidents/1"))
                .andExpect(jsonPath("$.incident.resourceId").value("p-2"))
                .andExpect(jsonPath("$.options.length()").value(4))
                .andExpect(jsonPath("$.decision.responsibleId").value("p-1"))
                .andExpect(jsonPath("$.decision.justification").value("A spare cooler is on its way"))
                .andExpect(jsonPath("$.decision.chosenOptionNumber").value(1))
                .andExpect(jsonPath("$.decision.alternativeExpeditionId").value(alternative));

        api.perform(get("/api/expeditions/{id}/replan-proposals", original))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(proposal));

        api.perform(get("/api/expeditions/{id}", original))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REPLACED"))
                .andExpect(jsonPath("$.replacementId").value(alternative));
        api.perform(get("/api/expeditions/{id}", alternative))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.replacedExpeditionId").value(original));

        api.perform(get("/api/expeditions/{id}/itinerary", original))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes[0].activity.scheduledPeriod.start").value("2026-10-01T13:00:00"));
        for (final var expedition : new String[] {original, alternative}) {
            api.perform(get("/api/expeditions/{id}/report", expedition))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.incidents.length()").value(1))
                    .andExpect(jsonPath("$.incidents[0].id").value(original + "/incidents/1"))
                    .andExpect(jsonPath("$.incidents[0].expectedDelayMinutes").value(120));
        }
    }

    @Test
    void decidingAsSomeoneWhoIsNotAResponsibleOfTheExpeditionAnswersUnprocessableEntity() throws Exception {
        final var expedition = anExpeditionInExecutionWithTheSampleCollectionStarted();
        final var proposal = aProposalFor(expedition, theCoolerFailedTakingLeeOutFor("120"));

        accept(proposal, 1, "p-3", "I happened to be around")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail")
                        .value("p-3 is not a responsible of the expedition " + expedition));
        reject(proposal, "p-3", "I happened to be around")
                .andExpect(status().isUnprocessableEntity());
        api.perform(get("/api/replan-proposals/{id}", proposal))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void decidingAProposalThatWasAlreadyDecidedAnswersConflict() throws Exception {
        final var expedition = anExpeditionInExecutionWithTheSampleCollectionStarted();
        final var proposal = aProposalFor(expedition, theCoolerFailedTakingLeeOutFor("120"));

        reject(proposal, "p-1", "The plan holds").andExpect(status().isNoContent());

        reject(proposal, "p-1", "Changed my mind")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("The replan proposal %s is no longer pending: it is REJECTED".formatted(proposal)));
        accept(proposal, 1, "p-1", "Changed my mind")
                .andExpect(status().isConflict());
    }

    @Test
    void acceptingAnOptionTheProposalDoesNotOfferAnswersNotFound() throws Exception {
        final var expedition = anExpeditionInExecutionWithTheSampleCollectionStarted();
        final var proposal = aProposalFor(expedition, theCoolerFailedTakingLeeOutFor("120"));

        accept(proposal, 99, "p-1", "Not an option")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail")
                        .value("There is no replan option with id 99 of the proposal " + proposal));
    }

    @Test
    void answersNotFoundForAProposalOrAnExpeditionThatDoesNotExist() throws Exception {
        api.perform(get("/api/replan-proposals/{id}", "rp-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no replan proposal with id rp-404"));
        api.perform(get("/api/expeditions/{id}/replan-proposals", "e-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no expedition with id e-404"));
        accept("rp-404", 1, "p-1", "Nothing to accept").andExpect(status().isNotFound());
        reject("rp-404", "p-1", "Nothing to reject").andExpect(status().isNotFound());
        api.perform(post("/api/expeditions/{id}/incidents", "e-404")
                        .contentType(MediaType.APPLICATION_JSON).content(theCoolerFailedTakingLeeOutFor("120")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no expedition with id e-404"));
    }

    @Test
    void registeringAnIncidentTheExpeditionDoesNotAcceptAnswersBadRequest() throws Exception {
        final var expedition = anExpeditionInExecutionWithTheSampleCollectionStarted();

        registerIncident(expedition, """
                        {"activityId": "a-1", "description": "The spare cooler failed too",
                         "occurredAt": "2026-10-01T13:30:00", "resourceId": "p-3"}
                        """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("p-3 is not assigned to Sample collection a-1"));

        finishTheStartedActivity(expedition);
        registerIncident(expedition, theCoolerFailedTakingLeeOutFor("120"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Sample collection a-1 already finished"));
    }

    private String theCoolerFailedTakingLeeOutFor(String delayMinutes) {
        return """
                {
                  "activityId": "a-1",
                  "description": "The cooler stopped working and Lee went back to base",
                  "occurredAt": "2026-10-01T13:30:00",
                  "resourceId": "p-2",
                  "expectedDelayMinutes": %s
                }
                """.formatted(delayMinutes);
    }

    private ResultActions registerIncident(String expeditionId, String body) throws Exception {
        return api.perform(post("/api/expeditions/{expeditionId}/incidents", expeditionId)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String aProposalFor(String expeditionId, String incident) throws Exception {
        final var registered = registerIncident(expeditionId, incident)
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(registered, "$.id");
    }

    private ResultActions accept(String proposalId, int optionNumber, String responsibleId, String justification)
            throws Exception {
        return api.perform(post("/api/replan-proposals/{proposalId}/acceptance", proposalId)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"optionNumber": %d, "responsibleId": "%s", "justification": "%s",
                         "decidedAt": "2026-10-01T13:45:00"}
                        """.formatted(optionNumber, responsibleId, justification)));
    }

    private ResultActions reject(String proposalId, String responsibleId, String justification) throws Exception {
        return api.perform(post("/api/replan-proposals/{proposalId}/rejection", proposalId)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"responsibleId": "%s", "justification": "%s", "decidedAt": "2026-10-01T13:45:00"}
                        """.formatted(responsibleId, justification)));
    }

    private void finishTheStartedActivity(String expeditionId) throws Exception {
        api.perform(post("/api/expeditions/{expeditionId}/activities/{activityId}/completion", expeditionId, "a-1")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"at": "2026-10-01T14:00:00", "result": "Nothing collected: the cooler failed"}
                                """))
                .andExpect(status().isNoContent());
    }

    private ResultActions transition(String expeditionId, String subResource) throws Exception {
        return api.perform(post("/api/expeditions/{expeditionId}/{sub}", expeditionId, subResource)
                .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions approve(String expeditionId) throws Exception {
        return api.perform(post("/api/expeditions/{expeditionId}/approval", expeditionId)
                .contentType(MediaType.APPLICATION_JSON).content("{}"));
    }

    private void assertStatus(String expeditionId, String expected) throws Exception {
        api.perform(get("/api/expeditions/{id}", expeditionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(expected));
    }

    private ResultActions register(String path, String body) throws Exception {
        return api.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String anExpeditionInExecutionWithTheSampleCollectionStarted() throws Exception {
        register("/api/zones", """
                {"id": "z-1", "name": "Coastal reef", "requiredPermits": []}
                """).andExpect(status().isCreated());
        register("/api/staff", """
                {"id": "p-1", "name": "Ada", "certifications": []}
                """).andExpect(status().isCreated());
        register("/api/staff", """
                {"id": "p-2", "name": "Lee", "certifications": []}
                """).andExpect(status().isCreated());
        register("/api/staff", """
                {"id": "p-3", "name": "Max", "certifications": []}
                """).andExpect(status().isCreated());
        register("/api/instruments", """
                {"id": "i-1", "name": "Field cooler", "category": "Cooler", "certifications": []}
                """).andExpect(status().isCreated());
        register("/api/supplies", """
                {"id": "s-1", "name": "Sample containers", "category": "Sample container",
                 "initialStock": {"amount": "5", "unit": "UNITS"}}
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
        final String expedition = JsonPath.read(created, "$.id");

        api.perform(post("/api/expeditions/{expeditionId}/activities", expedition)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {
                                  "id": "a-1",
                                  "name": "Sample collection a-1",
                                  "timeWindow": {"start": "2026-10-01T08:00:00", "end": "2026-10-01T18:00:00"},
                                  "scheduledPeriod": {"start": "2026-10-01T13:00:00", "end": "2026-10-01T15:00:00"},
                                  "zoneId": "z-1",
                                  "dependencyIds": [],
                                  "type": "Sample collection",
                                  "parameters": {"sampleCount": 1},
                                  "atNight": false
                                }
                                """))
                .andExpect(status().isCreated());
        for (final var assignment : new String[][] {{"staff", "p-2"}, {"equipment", "i-1"}, {"supplies", "s-1"}}) {
            api.perform(post("/api/expeditions/{expeditionId}/activities/{activityId}/{kind}",
                            expedition, "a-1", assignment[0])
                            .contentType(MediaType.APPLICATION_JSON).content("""
                                    {"resourceId": "%s"}
                                    """.formatted(assignment[1])))
                    .andExpect(status().isNoContent());
        }

        transition(expedition, "review").andExpect(status().isNoContent());
        approve(expedition).andExpect(status().isNoContent());
        transition(expedition, "execution").andExpect(status().isNoContent());
        api.perform(post("/api/expeditions/{expeditionId}/activities/{activityId}/start", expedition, "a-1")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"at": "2026-10-01T13:00:00"}
                                """))
                .andExpect(status().isNoContent());
        return expedition;
    }
}
