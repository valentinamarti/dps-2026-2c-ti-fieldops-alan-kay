package ar.edu.itba.dps.fieldops.business.fixtures;

import ar.edu.itba.dps.fieldops.business.exceptions.DuplicateReplanProposalException;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentId;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposal;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalId;
import ar.edu.itba.dps.fieldops.business.providers.ReplanProposalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What every {@link ReplanProposalRepository} must do, whatever it stores the proposals in.
 * Each adapter extends this class and supplies a fresh repository.
 */
public abstract class ReplanProposalRepositoryContract {

    private final ExpeditionId expedition = new ExpeditionId("e-1");
    private final ExpeditionId otherExpedition = new ExpeditionId("e-2");
    protected ReplanProposalRepository repository;

    protected abstract ReplanProposalRepository createRepository();

    @BeforeEach
    void setUpRepository() {
        repository = createRepository();
    }

    private ReplanProposal proposalFor(ExpeditionId original, int incidentNumber) {
        final var incident = new Incident(new IncidentId(original, incidentNumber), new ActivityId("a-1"), "Engine failure",
                LocalDateTime.of(2026, 10, 1, 9, 0), Optional.empty(), Optional.empty());
        return new ReplanProposal(repository.nextId(), original, incident, List.of());
    }

    @Test
    void neverHandsOutTheSameIdTwice() {
        final var ids = new HashSet<ReplanProposalId>();

        IntStream.range(0, 100).forEach(_ -> ids.add(repository.nextId()));

        assertEquals(100, ids.size());
    }

    @Test
    void rejectsAProposalThatWasAlreadyAdded() {
        final var proposal = proposalFor(expedition, 1);
        repository.add(proposal);

        assertThrows(DuplicateReplanProposalException.class, () -> repository.add(proposal));
    }

    @Test
    void findsTheVeryProposalThatWasAdded() {
        final var proposal = proposalFor(expedition, 1);
        repository.add(proposal);

        assertSame(proposal, repository.findById(proposal.getId()).orElseThrow());
    }

    @Test
    void findsNothingForAnUnknownId() {
        assertEquals(Optional.empty(), repository.findById(repository.nextId()));
    }

    @Test
    void findsTheProposalMadeForAnIncident() {
        final var first = proposalFor(expedition, 1);
        final var second = proposalFor(expedition, 2);
        repository.add(first);
        repository.add(second);

        assertSame(second, repository.findByIncident(new IncidentId(expedition, 2)).orElseThrow());
        assertEquals(Optional.empty(), repository.findByIncident(new IncidentId(otherExpedition, 2)));
    }

    @Test
    void listsOnlyTheProposalsOfAnExpeditionInTheOrderTheyWereAdded() {
        final var first = proposalFor(expedition, 1);
        final var other = proposalFor(otherExpedition, 1);
        final var second = proposalFor(expedition, 2);
        repository.add(first);
        repository.add(other);
        repository.add(second);

        assertEquals(List.of(first, second), repository.findByExpedition(expedition));
        assertTrue(repository.findByExpedition(new ExpeditionId("e-3")).isEmpty());
    }
}
