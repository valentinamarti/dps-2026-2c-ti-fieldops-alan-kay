package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * springdoc publishes the contract of the controllers. It is tested because the compatible line with Spring
 * Boot 4 is springdoc 3.x, and a build that compiles does not prove that the document is generated.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc api;

    @Test
    void publishesTheExpeditionEndpointsInTheOpenApiDocument() throws Exception {
        api.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Field Ops API"))
                .andExpect(jsonPath("$.paths['/api/expeditions'].post").exists())
                .andExpect(jsonPath("$.paths['/api/expeditions'].get").exists())
                .andExpect(jsonPath("$.paths['/api/expeditions/{id}'].get").exists())
                .andExpect(jsonPath("$.components.schemas.ExpeditionResponse").exists())
                .andExpect(jsonPath("$.components.schemas.CreateExpeditionRequest").exists());
    }

    @Test
    void publishesTheCatalogAndTheZonesWithASubResourceForTheUnavailabilities() throws Exception {
        api.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/staff'].post").exists())
                .andExpect(jsonPath("$.paths['/api/vehicles/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/instruments'].post").exists())
                .andExpect(jsonPath("$.paths['/api/supplies'].get").exists())
                .andExpect(jsonPath("$.paths['/api/equipment'].get").exists())
                .andExpect(jsonPath("$.paths['/api/zones/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/staff/{id}/unavailabilities'].post").exists())
                .andExpect(jsonPath("$.paths['/api/supplies/{id}/unavailabilities']").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.EquipmentCatalogResponse").exists())
                .andExpect(jsonPath("$.components.schemas.UnavailabilityDto").exists());
    }

    @Test
    void publishesTheActivitiesOfAnExpeditionAndTheTypesTheyCanBeScheduledAs() throws Exception {
        api.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/expeditions/{expeditionId}/activities'].post").exists())
                .andExpect(jsonPath("$.paths['/api/activity-types'].get").exists())
                .andExpect(jsonPath("$.components.schemas.ScheduleActivityRequest").exists())
                .andExpect(jsonPath("$.components.schemas.ScheduledActivityResponse").exists())
                .andExpect(jsonPath("$.components.schemas.ActivityTypeResponse").exists());
    }

    @Test
    void publishesTheBlocksTheItineraryAndTheValidationAndEstimationOfAnExpedition() throws Exception {
        api.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/expeditions/{expeditionId}/blocks'].post").exists())
                .andExpect(jsonPath("$.paths['/api/expeditions/{expeditionId}/itinerary'].get").exists())
                .andExpect(jsonPath("$.paths['/api/expeditions/{expeditionId}/validation'].get").exists())
                .andExpect(jsonPath("$.paths['/api/expeditions/{expeditionId}/estimate'].get").exists())
                .andExpect(jsonPath("$.components.schemas.ScheduleBlockRequest").exists())
                .andExpect(jsonPath("$.components.schemas.ItineraryNodeResponse").exists())
                .andExpect(jsonPath("$.components.schemas.ItineraryResponse").exists())
                .andExpect(jsonPath("$.components.schemas.ValidationResponse").exists())
                .andExpect(jsonPath("$.components.schemas.EstimateResponse").exists());
    }

    @Test
    void publishesTheAssignmentOfResourcesAndTheCandidatesOfAScheduledActivity() throws Exception {
        api.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/expeditions/{expeditionId}/activities/{activityId}/staff'].post").exists())
                .andExpect(jsonPath("$.paths['/api/expeditions/{expeditionId}/activities/{activityId}/equipment'].post").exists())
                .andExpect(jsonPath("$.paths['/api/expeditions/{expeditionId}/activities/{activityId}/supplies'].post").exists())
                .andExpect(jsonPath("$.paths['/api/expeditions/{expeditionId}/activities/{activityId}/candidates'].get").exists())
                .andExpect(jsonPath("$.components.schemas.AssignResourceRequest").exists())
                .andExpect(jsonPath("$.components.schemas.AssignmentCandidatesResponse").exists());
    }

    @Test
    void publishesTheLifecycleTheTrackingAndTheReportOfAnExpedition() throws Exception {
        api.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/expeditions/{expeditionId}/review'].post").exists())
                .andExpect(jsonPath("$.paths['/api/expeditions/{expeditionId}/draft'].post").exists())
                .andExpect(jsonPath("$.paths['/api/expeditions/{expeditionId}/approval'].post").exists())
                .andExpect(jsonPath("$.paths['/api/expeditions/{expeditionId}/execution'].post").exists())
                .andExpect(jsonPath("$.paths['/api/expeditions/{expeditionId}/suspension'].post").exists())
                .andExpect(jsonPath("$.paths['/api/expeditions/{expeditionId}/resumption'].post").exists())
                .andExpect(jsonPath("$.paths['/api/expeditions/{expeditionId}/completion'].post").exists())
                .andExpect(jsonPath("$.paths['/api/expeditions/{expeditionId}/report'].get").exists())
                .andExpect(jsonPath("$.paths['/api/expeditions/{expeditionId}/activities/{activityId}/start'].post").exists())
                .andExpect(jsonPath("$.paths['/api/expeditions/{expeditionId}/activities/{activityId}/completion'].post").exists())
                .andExpect(jsonPath("$.paths['/api/expeditions/{expeditionId}/activities/{activityId}/observations'].post").exists())
                .andExpect(jsonPath("$.components.schemas.ApprovalRequest").exists())
                .andExpect(jsonPath("$.components.schemas.ExpeditionReportResponse").exists());
    }

    @Test
    void publishesTheIncidentsAndTheReplanProposalsTheyGenerate() throws Exception {
        api.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/expeditions/{expeditionId}/incidents'].post").exists())
                .andExpect(jsonPath("$.paths['/api/expeditions/{expeditionId}/replan-proposals'].get").exists())
                .andExpect(jsonPath("$.paths['/api/replan-proposals/{proposalId}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/replan-proposals/{proposalId}/acceptance'].post").exists())
                .andExpect(jsonPath("$.paths['/api/replan-proposals/{proposalId}/rejection'].post").exists())
                .andExpect(jsonPath("$.components.schemas.RegisterIncidentRequest").exists())
                .andExpect(jsonPath("$.components.schemas.ReplanProposalResponse").exists())
                .andExpect(jsonPath("$.components.schemas.ReplanOptionDto").exists())
                .andExpect(jsonPath("$.components.schemas.AcceptReplanOptionRequest").exists())
                .andExpect(jsonPath("$.components.schemas.ReplanDecisionRequest").exists());
    }

    @Test
    void servesSwaggerUi() throws Exception {
        api.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
