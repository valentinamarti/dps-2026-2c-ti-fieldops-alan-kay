package ar.edu.itba.dps.fieldops.business.integration;

import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentReport;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ResourceAssignment;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanOption;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalStatus;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.responses.ReplanProposalView;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.ports.in.ApproveExpeditionPort;
import ar.edu.itba.dps.fieldops.business.ports.in.DecideReplanProposalPort;
import ar.edu.itba.dps.fieldops.business.ports.in.QueryExpeditionsPort;
import ar.edu.itba.dps.fieldops.business.ports.in.QueryReplanProposalsPort;
import ar.edu.itba.dps.fieldops.business.ports.in.RegisterIncidentPort;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ReplanProposalRepository;
import ar.edu.itba.dps.fieldops.business.replanning.ResourceReplaced;
import ar.edu.itba.dps.fieldops.business.models.commands.RegisterIncidentCommand;
import ar.edu.itba.dps.fieldops.business.models.commands.ReplanDecisionCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.instrument;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.supply;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;
import static ar.edu.itba.dps.fieldops.business.models.common.Quantity.of;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class IncidentReplanningIntegrationTest {
    private static final LocalDateTime TEN = LocalDateTime.of(2026, 10, 1, 10, 0);

    @Autowired
    private ExpeditionRepository expeditions;
    @Autowired
    private ReplanProposalRepository proposals;
    @Autowired
    private ApproveExpeditionPort approveExpedition;
    @Autowired
    private RegisterIncidentPort registerIncident;
    @Autowired
    private DecideReplanProposalPort decideReplanProposal;
    @Autowired
    private QueryReplanProposalsPort queryReplanProposals;
    @Autowired
    private QueryExpeditionsPort queryExpeditions;
    @Autowired
    private ResourceCatalog catalog;

    private final Zone coast = zone("z-1");
    private final Person leader = person("p-1");
    private final Person sick = person("p-2");
    private final Person spare = person("p-3");
    private final Activity sampling = sampling("a-1", coast, hours(8, 18));

    private Expedition approvedAndRunning() {
        List.of(leader, sick, spare).forEach(catalog::addStaff);
        final var expedition = expeditionIn(coast, leader).build(expeditions);
        expeditions.add(expedition);
        final var item = expedition.schedule(sampling, hours(13, 15));
        expedition.assignStaff(item, sick);
        expedition.assignEquipment(item, instrument("c-1", "Cooler"));
        expedition.assignSupply(item, supply("j-1", "Sample container", of("10", UNITS)));
        expedition.submitForReview();
        approveExpedition.approve(expedition.getId(), List.of());
        expedition.start();
        return expedition;
    }

    @Test
    void anIncidentInTheFieldEndsInAnApprovedAlternativeWithTheWholeHistoryQueryable() {
        final var original = approvedAndRunning();

        final var proposal = proposals.findById(registerIncident.register(new RegisterIncidentCommand(original.getId(), sampling.getId(),
                IncidentReport.of("Fever", TEN).affecting(sick.getId()))).id()).orElseThrow();

        assertEquals(List.of(new ResourceReplaced(sick.getId(), ResourceAssignment.of(leader)),
                        new ResourceReplaced(sick.getId(), ResourceAssignment.of(spare))),
                proposal.getOptions().stream().map(ReplanOption::reason).limit(2).toList());

        final var alternative = expeditions.findById(decideReplanProposal.accept(proposal.getId(), 2,
                new ReplanDecisionCommand(leader.getId(), "p-3 knows the site", TEN.plusMinutes(15))).id()).orElseThrow();
        alternative.submitForReview();
        approveExpedition.approve(alternative.getId(), List.of());

        final var audited = queryReplanProposals.get(proposal.getId());
        assertEquals(ReplanProposalStatus.ACCEPTED, audited.status());
        assertEquals(Optional.of(alternative.getId()), audited.decision().orElseThrow().alternative());
        assertEquals("p-3 knows the site", audited.decision().orElseThrow().justification());
        assertEquals(ExpeditionStatus.REPLACED, queryExpeditions.get(original.getId()).status());
        final var replaced = expeditions.findById(original.getId()).orElseThrow();
        assertEquals(Set.of(sick), replaced.getItinerary().getItems().getFirst().getAssignedStaff());
        assertEquals(Set.of(spare), alternative.getItinerary().getItems().getFirst().getAssignedStaff());
        assertEquals(List.of(ReplanProposalView.of(proposal)), queryReplanProposals.ofExpedition(original.getId()));
    }
}
