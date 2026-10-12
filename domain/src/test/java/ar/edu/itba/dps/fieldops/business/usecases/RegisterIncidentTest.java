package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.InvalidTrackingException;
import ar.edu.itba.dps.fieldops.business.exceptions.NotFoundException;
import ar.edu.itba.dps.fieldops.business.models.events.IncidentRegistered;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentReport;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposal;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalId;
import ar.edu.itba.dps.fieldops.business.providers.EventPublisher;
import ar.edu.itba.dps.fieldops.business.providers.ReplanProposalRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static ar.edu.itba.dps.fieldops.business.usecases.IncidentScenario.TEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterIncidentTest {

    private final IncidentScenario scenario = new IncidentScenario();
    private final EventPublisher publisher = mock(EventPublisher.class);
    private final ReplanProposalRepository proposals = mock(ReplanProposalRepository.class);
    private final RegisterIncident registerIncident = new RegisterIncident(scenario.expeditions, publisher, proposals);

    @Test
    void storesTheIncidentInTheExpeditionPublishesItAndReturnsTheProposalMadeForIt() {
        final var expected = mock(ReplanProposal.class);
        when(proposals.findByIncident(any())).thenReturn(Optional.of(expected));

        final var proposal = registerIncident.register(scenario.incidentOn(scenario.afternoon, IncidentReport.of("Storm forecast", TEN)));

        final var incident = scenario.expedition.getIncidents().getFirst();
        verify(publisher).publish(new IncidentRegistered(scenario.expedition.getId(), incident));
        verify(proposals).findByIncident(incident.id());
        assertSame(expected, proposal);
    }

    @Test
    void failsIfNobodyMadeAProposalForTheIncident() {
        when(proposals.findByIncident(any())).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> registerIncident.register(scenario.incidentOn(scenario.afternoon, IncidentReport.of("Storm forecast", TEN))));
    }

    @Test
    void anUnknownExpeditionPublishesNothing() {
        final var command = new RegisterIncidentCommand(new ExpeditionId("e-99"), scenario.afternoon.getId(), IncidentReport.of("Storm", TEN));

        assertThrows(NotFoundException.class, () -> registerIncident.register(command));
        verify(publisher, never()).publish(any());
    }

    @Test
    void anIncidentTheExpeditionRejectsPublishesNothing() {
        scenario.expedition.suspend();

        assertThrows(InvalidTrackingException.class,
                () -> registerIncident.register(scenario.incidentOn(scenario.afternoon, IncidentReport.of("Storm", TEN))));
        verify(publisher, never()).publish(any());
        assertEquals(List.of(), scenario.expedition.getIncidents());
    }

    @Test
    void theProposalSubscriberStoresOneProposalPerEvent() {
        final var stored = mock(ReplanProposalRepository.class);
        when(stored.nextId()).thenReturn(new ReplanProposalId("rp-1"));
        final var subscriber = new ProposeReplan(scenario.expeditions, stored, scenario.replanner, List.of());
        final var incident = scenario.expedition.registerIncident(scenario.itemOf(scenario.expedition, scenario.afternoon),
                IncidentReport.of("Storm", TEN));

        subscriber.on(new IncidentRegistered(scenario.expedition.getId(), incident));

        verify(stored).add(any(ReplanProposal.class));
        assertEquals(IncidentRegistered.class, subscriber.eventType());
    }
}
