package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.commands.PlannedActivityCommand;
import ar.edu.itba.dps.fieldops.business.models.commands.ScheduleActivityCommand;
import ar.edu.itba.dps.fieldops.business.models.commands.ScheduleBlockCommand;
import ar.edu.itba.dps.fieldops.business.models.commands.PlannedNodeCommand;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentReport;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Observation;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposal;
import ar.edu.itba.dps.fieldops.business.models.resources.Unavailability;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;
import ar.edu.itba.dps.fieldops.business.rules.ActivityTypeRegistry;
import ar.edu.itba.dps.fieldops.business.services.ResourceReservations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.APPROVED_AT;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.usecases.IncidentScenario.TEN;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UseCasesSaveTheirChangesTest {
    private final IncidentScenario scenario = new IncidentScenario();
    private final ZoneRepository zones = mock(ZoneRepository.class);
    private final ActivityTypeRegistry types = ActivityTypeRegistry.withStandardTypes();
    private final TrackActivity tracking = new TrackActivity(scenario.expeditions, scenario.catalog);
    private Expedition draft;

    private static LocalDateTime at(int hour) {
        return LocalDateTime.of(2026, 10, 1, hour, 0);
    }

    @BeforeEach
    void setUp() {
        draft = ExpeditionFixtures.createExpeditionOn(scenario.expeditions).create(expeditionIn(scenario.coast, scenario.leader));
        when(zones.findById(scenario.coast.getId())).thenReturn(Optional.of(scenario.coast));
        clearInvocations(scenario.expeditions, scenario.proposals, scenario.catalog);
    }

    private PlannedActivityCommand sampling(String id) {
        return new PlannedActivityCommand(new ActivityId(id), "Sampling " + id, hours(0, 24), hours(9, 11),
                scenario.coast.getId(), List.of(), "Sample collection", Map.of("sampleCount", 3), false);
    }

    private ReplanProposal storm() {
        return scenario.registered(scenario.incidentOn(scenario.afternoon, IncidentReport.of("Storm", TEN).delayingBy(Duration.ofHours(1))));
    }

    @Test
    void schedulingAnActivitySavesTheExpedition() {
        new ScheduleActivity(scenario.expeditions, zones, types).schedule(new ScheduleActivityCommand(draft.getId(), sampling("a-9")));

        verify(scenario.expeditions).save(draft);
    }

    @Test
    void schedulingABlockSavesTheExpedition() {
        new ScheduleBlock(scenario.expeditions, zones, types).schedule(new ScheduleBlockCommand(draft.getId(),
                PlannedNodeCommand.parallel(PlannedNodeCommand.activity(sampling("a-9")))));

        verify(scenario.expeditions).save(draft);
    }

    @Test
    void assigningAResourceSavesTheExpedition() {
        draft.schedule(scenario.morning, hours(9, 11));

        new AssignResources(scenario.expeditions, scenario.catalog)
                .assignStaff(draft.getId(), scenario.morning.getId(), scenario.leader.getId());

        verify(scenario.expeditions).save(draft);
    }

    @Test
    void aTransitionSavesTheExpedition() {
        new TransitionExpedition(scenario.expeditions).suspend(scenario.expedition.getId());

        verify(scenario.expeditions).save(scenario.expedition);
    }

    @Test
    void trackingAnActivitySavesTheExpeditionAndWhatItConsumed() {
        final var id = scenario.expedition.getId();
        final var morning = scenario.morning.getId();

        tracking.start(id, morning, at(9));
        tracking.observe(id, morning, new Observation("Calm seas", at(10)));
        tracking.finish(id, morning, at(11), "All samples");

        verify(scenario.expeditions, times(3)).save(scenario.expedition);
        scenario.itemOf(scenario.expedition, scenario.morning).getAssignedSupplies()
                .forEach(supply -> verify(scenario.catalog).save(supply));
    }

    @Test
    void approvingAnAlternativeSavesBothExpeditionsAndTheirResources() {
        final var alternative = scenario.accepted(storm().getId(), 1, scenario.decisionBy(scenario.leader, "We wait"));
        alternative.submitForReview();
        clearInvocations(scenario.expeditions, scenario.catalog);

        scenario.approveExpedition.approve(alternative.getId(), List.of(), APPROVED_AT);

        verify(scenario.expeditions).save(alternative);
        verify(scenario.expeditions).save(scenario.expedition);
        alternative.assignedResources().forEach(resource -> verify(scenario.catalog, atLeastOnce()).save(resource));
    }

    @Test
    void finishingAnExpeditionSavesItAndItsResources() {
        new FinishExpedition(new ResourceReservations(), scenario.expeditions, scenario.catalog).finish(scenario.expedition.getId());

        verify(scenario.expeditions).save(scenario.expedition);
        scenario.expedition.assignedResources().forEach(resource -> verify(scenario.catalog).save(resource));
    }

    @Test
    void acceptingAProposalSavesTheOriginalTheProposalAndTheOnesItSupersedes() {
        final var accepted = storm();
        final var superseded = scenario.registered(scenario.incidentOn(scenario.morning, IncidentReport.of("Fog", TEN)));
        clearInvocations(scenario.expeditions, scenario.proposals);

        scenario.decide.accept(accepted.getId(), 1, scenario.decisionBy(scenario.leader, "We wait"));

        verify(scenario.expeditions).save(scenario.expedition);
        verify(scenario.proposals).save(accepted);
        verify(scenario.proposals).save(superseded);
    }

    @Test
    void rejectingAProposalSavesIt() {
        final var proposal = storm();

        scenario.decide.reject(proposal.getId(), scenario.decisionBy(scenario.leader, "The plan holds"));

        verify(scenario.proposals).save(proposal);
    }

    @Test
    void markingAResourceUnavailableSavesIt() {
        new MarkResourceUnavailable(scenario.catalog).markStaffUnavailable(scenario.leader.getId(), new Unavailability(hours(9, 10), "Medical leave"));

        verify(scenario.catalog).save(scenario.leader);
    }
}
