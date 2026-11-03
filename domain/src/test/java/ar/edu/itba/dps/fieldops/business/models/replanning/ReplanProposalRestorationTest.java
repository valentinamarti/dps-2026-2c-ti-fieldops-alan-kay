package ar.edu.itba.dps.fieldops.business.models.replanning;

import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentId;
import ar.edu.itba.dps.fieldops.business.replanning.Cancellation;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ReplanProposalRestorationTest {
    private static final LocalDateTime NOON = LocalDateTime.of(2026, 10, 1, 12, 0);
    private static final ExpeditionId ORIGINAL = new ExpeditionId("e-1");

    @Test
    void anAcceptedProposalKeepsItsDecisionAndItsAlternative() {
        final var incident = new Incident(new IncidentId(ORIGINAL, 1), new ActivityId("a-1"), "Storm", NOON, Optional.empty(), Optional.empty());
        final var proposal = new ReplanProposal(new ReplanProposalId("rp-1"), ORIGINAL, incident,
                List.of(new ReplanOption(new Cancellation(), "Cancel it")));
        final var alternative = new ExpeditionId("e-2");
        proposal.accept(1, new ReplanDecision(person("p-1"), "No safe window", NOON), alternative);

        final var restored = ReplanProposal.restore(proposal.snapshot());

        assertEquals(proposal.snapshot(), restored.snapshot());
        assertEquals(ReplanProposalStatus.ACCEPTED, restored.getStatus());
        assertEquals(Optional.of(alternative), restored.getAlternative());
        assertEquals(Optional.of(1), restored.getChosenOptionNumber());
    }
}
