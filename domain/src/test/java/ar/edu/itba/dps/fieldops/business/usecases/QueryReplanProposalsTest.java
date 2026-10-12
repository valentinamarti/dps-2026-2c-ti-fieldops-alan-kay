package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.NotFoundException;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentReport;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ar.edu.itba.dps.fieldops.business.usecases.IncidentScenario.TEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryReplanProposalsTest {

    private final IncidentScenario scenario = new IncidentScenario();

    @Test
    void getsAProposalByIdAndListsTheOnesOfAnExpedition() {
        final var first = scenario.registerIncident.register(scenario.incidentOn(scenario.morning, IncidentReport.of("Jar broke", TEN)));
        final var second = scenario.registerIncident.register(scenario.incidentOn(scenario.afternoon, IncidentReport.of("Storm", TEN)));

        assertSame(second, scenario.queryProposals.get(second.getId()));
        assertEquals(List.of(first, second), scenario.queryProposals.ofExpedition(scenario.expedition.getId()));
    }

    @Test
    void anExpeditionWithoutIncidentsHasNoProposals() {
        assertEquals(List.of(), scenario.queryProposals.ofExpedition(scenario.expedition.getId()));
    }

    @Test
    void unknownIdsAreNotFound() {
        assertThrows(NotFoundException.class, () -> scenario.queryProposals.get(new ReplanProposalId("rp-9")));
        assertThrows(NotFoundException.class, () -> scenario.queryProposals.ofExpedition(new ExpeditionId("e-9")));
    }
}
