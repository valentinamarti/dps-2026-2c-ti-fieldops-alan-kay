package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.ActivitiesInProgressException;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidScheduleException;
import ar.edu.itba.dps.fieldops.business.exceptions.NotAResponsibleException;
import ar.edu.itba.dps.fieldops.business.exceptions.NotFoundException;
import ar.edu.itba.dps.fieldops.business.exceptions.ProposalAlreadyDecidedException;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentReport;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposal;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalId;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.usecases.IncidentScenario.TEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecideReplanProposalTest {

    private final IncidentScenario scenario = new IncidentScenario();

    private ReplanProposal stormOnTheAfternoon() {
        return scenario.registerIncident.register(scenario.incidentOn(scenario.afternoon,
                IncidentReport.of("Storm forecast", TEN).delayingBy(Duration.ofHours(1))));
    }

    @Test
    void onlyAResponsibleOfTheExpeditionDecides() {
        final var proposal = stormOnTheAfternoon();

        assertThrows(NotAResponsibleException.class,
                () -> scenario.decide.accept(proposal.getId(), 1, scenario.decisionBy(scenario.outsider, "I think so")));
        assertThrows(NotAResponsibleException.class,
                () -> scenario.decide.reject(proposal.getId(), scenario.decisionBy(scenario.outsider, "I think not")));
        assertTrue(proposal.isPending());
        assertEquals(ExpeditionStatus.IN_EXECUTION, scenario.expedition.getStatus());
    }

    @Test
    void aDecisionNeedsAJustification() {
        final var proposal = stormOnTheAfternoon();

        assertThrows(IllegalArgumentException.class,
                () -> scenario.decide.reject(proposal.getId(), scenario.decisionBy(scenario.leader, "")));
        assertTrue(proposal.isPending());
    }

    @Test
    void aProposalThatWasAlreadyDecidedCannotBeDecidedAgain() {
        final var proposal = stormOnTheAfternoon();
        scenario.decide.reject(proposal.getId(), scenario.decisionBy(scenario.leader, "We wait"));

        assertThrows(ProposalAlreadyDecidedException.class,
                () -> scenario.decide.accept(proposal.getId(), 1, scenario.decisionBy(scenario.leader, "Changed my mind")));
    }

    @Test
    void anUnknownProposalOrOptionIsNotFound() {
        final var proposal = stormOnTheAfternoon();

        assertThrows(NotFoundException.class,
                () -> scenario.decide.reject(new ReplanProposalId("rp-99"), scenario.decisionBy(scenario.leader, "No")));
        assertThrows(NotFoundException.class,
                () -> scenario.decide.accept(proposal.getId(), 9, scenario.decisionBy(scenario.leader, "Yes")));
    }

    @Test
    void acceptingWhileAnActivityIsInProgressChangesNothing() {
        final var proposal = stormOnTheAfternoon();
        scenario.expedition.startActivity(scenario.itemOf(scenario.expedition, scenario.morning), LocalDateTime.of(2026, 10, 1, 9, 0));

        assertThrows(ActivitiesInProgressException.class,
                () -> scenario.decide.accept(proposal.getId(), 1, scenario.decisionBy(scenario.leader, "Delay it")));
        assertEquals(ExpeditionStatus.IN_EXECUTION, scenario.expedition.getStatus());
        assertTrue(proposal.isPending());
        assertEquals(1, scenario.expeditions.findAll().size());
    }

    @Test
    void anIncidentOnAnActivityInProgressIsAcceptedOnceItIsClosedAndTheActivityIsRedone() {
        final var morningItem = scenario.itemOf(scenario.expedition, scenario.morning);
        scenario.expedition.startActivity(morningItem, LocalDateTime.of(2026, 10, 1, 9, 0));
        final var proposal = scenario.registerIncident.register(scenario.incidentOn(scenario.morning,
                IncidentReport.of("Current too strong", TEN).delayingBy(Duration.ofHours(2))));
        scenario.expedition.finishActivity(morningItem, TEN, "Aborted after one transect");

        final var alternative = scenario.decide.accept(proposal.getId(), 1, scenario.decisionBy(scenario.leader, "Redo it later"));

        final var redone = scenario.itemOf(alternative, scenario.morning);
        assertFalse(redone.isStarted());
        assertEquals(hours(11, 13), redone.getScheduledPeriod());
        assertTrue(morningItem.isFinished());
    }

    @Test
    void anOptionThatCanNoLongerBeBuiltIsRejectedWithoutChangingAnything() {
        final var proposal = stormOnTheAfternoon();
        final var dependentItem = scenario.itemOf(scenario.expedition, scenario.dependent);
        scenario.expedition.startActivity(dependentItem, LocalDateTime.of(2026, 10, 1, 10, 0));
        scenario.expedition.finishActivity(dependentItem, LocalDateTime.of(2026, 10, 1, 10, 30), "Done ahead of time");

        assertThrows(InvalidScheduleException.class,
                () -> scenario.decide.accept(proposal.getId(), 1, scenario.decisionBy(scenario.leader, "Delay it")));
        assertEquals(ExpeditionStatus.IN_EXECUTION, scenario.expedition.getStatus());
        assertTrue(proposal.isPending());
    }

    @Test
    void acceptingOneProposalSupersedesTheOtherPendingOnesOfTheExpedition() {
        final var first = stormOnTheAfternoon();
        final var second = scenario.registerIncident.register(scenario.incidentOn(scenario.morning, IncidentReport.of("Jar broke", TEN)));

        scenario.decide.accept(second.getId(), 1, scenario.decisionBy(scenario.leader, "Skip the morning"));

        assertEquals(ReplanProposalStatus.SUPERSEDED, first.getStatus());
        assertEquals(Optional.of(second.getId()), first.getSupersededBy());
        assertThrows(ProposalAlreadyDecidedException.class,
                () -> scenario.decide.accept(first.getId(), 1, scenario.decisionBy(scenario.leader, "Also this")));
    }

    @Test
    void anAcceptedAlternativeThatIsNeverApprovedLeavesTheOriginalSuspendedToBeResumed() {
        final var proposal = stormOnTheAfternoon();
        scenario.decide.accept(proposal.getId(), 1, scenario.decisionBy(scenario.leader, "Delay it"));

        scenario.expedition.resume();

        assertEquals(ExpeditionStatus.IN_EXECUTION, scenario.expedition.getStatus());
    }
}
