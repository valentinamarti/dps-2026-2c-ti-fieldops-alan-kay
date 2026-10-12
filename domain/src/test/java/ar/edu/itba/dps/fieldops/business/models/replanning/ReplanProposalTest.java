package ar.edu.itba.dps.fieldops.business.models.replanning;

import ar.edu.itba.dps.fieldops.business.exceptions.NotFoundException;
import ar.edu.itba.dps.fieldops.business.exceptions.ProposalAlreadyDecidedException;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentId;
import ar.edu.itba.dps.fieldops.business.replanning.Cancellation;
import ar.edu.itba.dps.fieldops.business.replanning.Delay;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplanProposalTest {

    private static final LocalDateTime NOON = LocalDateTime.of(2026, 10, 1, 12, 0);

    private final ExpeditionId original = new ExpeditionId("e-1");
    private final Incident incident = new Incident(new IncidentId(original, 1), new ActivityId("a-1"), "Engine failure",
            NOON, Optional.empty(), Optional.of(Duration.ofHours(1)));
    private final ReplanOption delay = new ReplanOption(new Delay(Duration.ofHours(1)), "Run a-1 1 h later");
    private final ReplanOption cancellation = new ReplanOption(new Cancellation(), "Cancel a-1");
    private final ReplanProposal proposal = new ReplanProposal(new ReplanProposalId("rp-1"), original, incident, List.of(delay, cancellation));
    private final ReplanDecision decision = new ReplanDecision(person("p-1"), "The engine will be fixed soon", NOON);

    @Test
    void startsPendingWithTheIncidentAndItsAffectedActivity() {
        assertTrue(proposal.isPending());
        assertEquals(ReplanProposalStatus.PENDING, proposal.getStatus());
        assertEquals(new ActivityId("a-1"), proposal.getAffectedActivity());
        assertEquals(Optional.empty(), proposal.getDecision());
    }

    @Test
    void numbersItsOptionsFromOne() {
        assertEquals(delay, proposal.option(1));
        assertEquals(cancellation, proposal.option(2));
        assertThrows(NotFoundException.class, () -> proposal.option(0));
        assertThrows(NotFoundException.class, () -> proposal.option(3));
    }

    @Test
    void acceptingRecordsTheDecisionTheChosenOptionAndTheAlternative() {
        proposal.accept(2, decision, new ExpeditionId("e-2"));

        assertEquals(ReplanProposalStatus.ACCEPTED, proposal.getStatus());
        assertEquals(Optional.of(decision), proposal.getDecision());
        assertEquals(Optional.of(cancellation), proposal.getChosenOption());
        assertEquals(Optional.of(2), proposal.getChosenOptionNumber());
        assertEquals(Optional.of(new ExpeditionId("e-2")), proposal.getAlternative());
    }

    @Test
    void acceptingAnOptionThatDoesNotExistChangesNothing() {
        assertThrows(NotFoundException.class, () -> proposal.accept(5, decision, new ExpeditionId("e-2")));

        assertTrue(proposal.isPending());
    }

    @Test
    void rejectingRecordsTheDecisionWithoutAnAlternative() {
        proposal.reject(decision);

        assertEquals(ReplanProposalStatus.REJECTED, proposal.getStatus());
        assertEquals(Optional.of(decision), proposal.getDecision());
        assertEquals(Optional.empty(), proposal.getChosenOption());
        assertEquals(Optional.empty(), proposal.getAlternative());
    }

    @Test
    void aSupersededProposalPointsToTheOneThatWasAccepted() {
        proposal.supersedeBy(new ReplanProposalId("rp-2"));

        assertEquals(ReplanProposalStatus.SUPERSEDED, proposal.getStatus());
        assertEquals(Optional.of(new ReplanProposalId("rp-2")), proposal.getSupersededBy());
        assertFalse(proposal.isPending());
    }

    @Test
    void aProposalIsDecidedOnlyOnce() {
        proposal.reject(decision);

        assertThrows(ProposalAlreadyDecidedException.class, () -> proposal.accept(1, decision, new ExpeditionId("e-2")));
        assertThrows(ProposalAlreadyDecidedException.class, () -> proposal.reject(decision));
        assertThrows(ProposalAlreadyDecidedException.class, () -> proposal.supersedeBy(new ReplanProposalId("rp-2")));
    }

    @Test
    void aDecisionNeedsAJustification() {
        assertThrows(IllegalArgumentException.class, () -> new ReplanDecision(person("p-1"), " ", NOON));
    }
}
