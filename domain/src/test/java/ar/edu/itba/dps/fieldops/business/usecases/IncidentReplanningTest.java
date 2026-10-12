package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentReport;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalStatus;
import ar.edu.itba.dps.fieldops.business.replanning.Cancellation;
import ar.edu.itba.dps.fieldops.business.replanning.Delay;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.usecases.IncidentScenario.TEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The acceptance criteria of F3, through the use cases.
 */
class IncidentReplanningTest {

    private final IncidentScenario scenario = new IncidentScenario();

    @Test
    void registeringAnIncidentOnAnExpeditionInExecutionGeneratesAReplanProposal() {
        final var proposal = scenario.registerIncident.register(scenario.incidentOn(scenario.afternoon,
                IncidentReport.of("Boat engine failed", TEN).delayingBy(Duration.ofHours(1))));

        assertEquals(ReplanProposalStatus.PENDING, proposal.getStatus());
        assertEquals(scenario.expedition.getId(), proposal.getOriginalExpedition());
        assertEquals(scenario.expedition.getIncidents(), List.of(proposal.getIncident()));
        assertEquals(List.of(new Delay(Duration.ofHours(1)), new Cancellation()),
                proposal.getOptions().stream().map(option -> option.reason()).toList());
        assertSame(proposal, scenario.proposals.findById(proposal.getId()).orElseThrow());
        assertEquals(ExpeditionStatus.IN_EXECUTION, scenario.expedition.getStatus());
    }

    @Test
    void theResponsibleCanAcceptAnOptionAndGetsTheAlternativeAsADraft() {
        final var proposal = scenario.registerIncident.register(scenario.incidentOn(scenario.afternoon,
                IncidentReport.of("Boat engine failed", TEN).delayingBy(Duration.ofHours(1))));

        final var alternative = scenario.decide.accept(proposal.getId(), 1,
                scenario.decisionBy(scenario.leader, "The mechanic arrives in an hour"));

        assertEquals(ReplanProposalStatus.ACCEPTED, proposal.getStatus());
        assertEquals(ExpeditionStatus.DRAFT, alternative.getStatus());
        assertEquals(ExpeditionStatus.SUSPENDED, scenario.expedition.getStatus());
        assertEquals(hours(14, 16), scenario.itemOf(alternative, scenario.afternoon).getScheduledPeriod());
        assertEquals(hours(17, 18), scenario.itemOf(alternative, scenario.dependent).getScheduledPeriod());
        assertEquals(Optional.of(scenario.expedition.getId()), alternative.getReplacedExpedition());
    }

    @Test
    void theResponsibleCanRejectTheProposalAndTheExpeditionKeepsRunningAsItWas() {
        final var proposal = scenario.registerIncident.register(scenario.incidentOn(scenario.afternoon,
                IncidentReport.of("Light rain", TEN)));

        scenario.decide.reject(proposal.getId(), scenario.decisionBy(scenario.leader, "Rain does not stop sampling"));

        assertEquals(ReplanProposalStatus.REJECTED, proposal.getStatus());
        assertEquals(ExpeditionStatus.IN_EXECUTION, scenario.expedition.getStatus());
        assertEquals(1, scenario.expeditions.findAll().size());
    }

    @Test
    void theOriginalPlanTheIncidentAndTheDecisionStayAuditedAndQueryable() {
        final var proposal = scenario.registerIncident.register(scenario.incidentOn(scenario.afternoon,
                IncidentReport.of("Storm forecast", TEN)));
        final var alternative = scenario.decide.accept(proposal.getId(), 1,
                scenario.decisionBy(scenario.leader, "No safe window this afternoon"));
        alternative.submitForReview();
        scenario.approveExpedition.approve(alternative, List.of());

        final var audited = scenario.queryProposals.ofExpedition(scenario.expedition.getId()).getFirst();
        final var original = new QueryExpeditions(scenario.expeditions).get(audited.getOriginalExpedition());
        assertEquals(ExpeditionStatus.REPLACED, original.getStatus());
        assertEquals(Optional.of(alternative.getId()), original.getReplacement());
        assertEquals(3, original.getItinerary().getItems().size());
        assertEquals("Storm forecast", audited.getIncident().description());
        assertEquals(List.of(audited.getIncident()), original.getIncidents());
        final var decision = audited.getDecision().orElseThrow();
        assertSame(scenario.leader, decision.responsible());
        assertEquals("No safe window this afternoon", decision.justification());
        assertEquals(TEN.plusMinutes(30), decision.decidedAt());
        assertEquals(Optional.of(new Cancellation()), audited.getChosenOption().map(option -> option.reason()));
        assertEquals(Optional.of(alternative.getId()), audited.getAlternative());
        assertTrue(scenario.itemOf(alternative, scenario.morning).getScheduledPeriod().equals(hours(9, 11)));
        assertTrue(alternative.getItinerary().itemFor(scenario.dependent).isEmpty());
        assertEquals(original.getIncidents(), alternative.getIncidents());
    }
}
