package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.fixtures.FakeResourceCatalog;
import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.fixtures.SequentialReplanProposalRepository;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.commands.RegisterIncidentCommand;
import ar.edu.itba.dps.fieldops.business.models.commands.ReplanDecisionCommand;
import ar.edu.itba.dps.fieldops.business.models.events.IncidentRegistered;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentReport;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposal;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalId;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.providers.EventPublisher;
import ar.edu.itba.dps.fieldops.business.replanning.CancellationOptions;
import ar.edu.itba.dps.fieldops.business.replanning.ExpeditionReplanner;
import ar.edu.itba.dps.fieldops.business.replanning.RescheduleOptions;
import ar.edu.itba.dps.fieldops.business.replanning.ResourceReplacementOptions;
import ar.edu.itba.dps.fieldops.business.services.ResourceReservations;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.spy;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.completeAssignments;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.createExpeditionOn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.oneHourSampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;

final class IncidentScenario {
    static final LocalDateTime TEN = LocalDateTime.of(2026, 10, 1, 10, 0);

    final SequentialExpeditionRepository expeditions = spy(new SequentialExpeditionRepository());
    final SequentialReplanProposalRepository proposals = spy(new SequentialReplanProposalRepository());
    final ResourceCatalog catalog = spy(new FakeResourceCatalog());
    final ResourceReservations reservations = new ResourceReservations();
    final ExpeditionReplanner replanner = new ExpeditionReplanner(expeditions);
    final ReplanExpedition replanExpedition = new ReplanExpedition(expeditions, replanner);
    final ApproveExpedition approveExpedition = new ApproveExpedition(List.of(), reservations, expeditions, catalog);
    final ProposeReplan proposeReplan = new ProposeReplan(expeditions, proposals, replanner,
            List.of(new RescheduleOptions(), new ResourceReplacementOptions(catalog), new CancellationOptions()));
    final EventPublisher publisher = event -> proposeReplan.on(IncidentRegistered.class.cast(event));
    final RegisterIncident registerIncident = new RegisterIncident(expeditions, publisher, proposals);
    final DecideReplanProposal decide = new DecideReplanProposal(proposals, expeditions, replanner, replanExpedition, catalog);
    final QueryReplanProposals queryProposals = new QueryReplanProposals(proposals, expeditions);

    final Zone coast = zone("z-1");
    final Person leader = person("p-1");
    final Person outsider = person("p-99");
    final Activity morning = sampling("a-1", coast, hours(8, 18));
    final Activity afternoon = sampling("a-2", coast, hours(8, 18));
    final Activity dependent = oneHourSampling("a-3", coast, hours(8, 18), afternoon);
    final Expedition expedition;

    IncidentScenario() {
        catalog.addStaff(leader);
        catalog.addStaff(outsider);
        expedition = createExpeditionOn(expeditions).create(expeditionIn(coast, leader));
        expedition.schedule(morning, hours(9, 11));
        expedition.schedule(afternoon, hours(13, 15));
        expedition.schedule(dependent, hours(16, 17));
        completeAssignments(expedition);
        expedition.submitForReview();
        approveExpedition.approve(expedition, List.of());
        expedition.start();
    }

    ReplanProposal registered(RegisterIncidentCommand command) {
        return proposals.findById(registerIncident.register(command).id()).orElseThrow();
    }

    Expedition accepted(ReplanProposalId proposal, int optionNumber, ReplanDecisionCommand command) {
        return expeditions.findById(decide.accept(proposal, optionNumber, command).id()).orElseThrow();
    }

    ItineraryItem itemOf(Expedition of, Activity activity) {
        return of.getItinerary().itemFor(activity).orElseThrow();
    }

    RegisterIncidentCommand incidentOn(Activity activity, IncidentReport report) {
        return new RegisterIncidentCommand(expedition.getId(), activity.getId(), report);
    }

    ReplanDecisionCommand decisionBy(Person responsible, String justification) {
        return new ReplanDecisionCommand(responsible.getId(), justification, TEN.plusMinutes(30));
    }
}
