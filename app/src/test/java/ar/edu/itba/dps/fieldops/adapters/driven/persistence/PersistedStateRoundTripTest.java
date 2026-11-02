package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionSnapshot;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentReport;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Observation;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ResourceAssignment;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanDecision;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanOption;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposal;
import ar.edu.itba.dps.fieldops.business.models.resources.CalendarSnapshot;
import ar.edu.itba.dps.fieldops.business.models.resources.Depletable;
import ar.edu.itba.dps.fieldops.business.models.resources.Instrument;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.Reservation;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.resources.Unavailability;
import ar.edu.itba.dps.fieldops.business.models.resources.Vehicle;
import ar.edu.itba.dps.fieldops.business.models.validation.AcceptedWarning;
import ar.edu.itba.dps.fieldops.business.models.validation.RuleId;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationSubject;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ReplanProposalRepository;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;
import ar.edu.itba.dps.fieldops.business.replanning.Cancellation;
import ar.edu.itba.dps.fieldops.business.replanning.Delay;
import ar.edu.itba.dps.fieldops.business.replanning.ExpeditionReplanner;
import ar.edu.itba.dps.fieldops.business.replanning.ResourceReplaced;
import ar.edu.itba.dps.fieldops.business.replanning.ResourceWithdrawn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.nightSampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.activity;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.parallel;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.sequential;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PersistedStateRoundTripTest {
    private static final ResourceCategory COOLER = new ResourceCategory("Cooler");
    private static final ResourceCategory CONTAINERS = new ResourceCategory("Sample container");
    private static final ExpeditionId OWNER = new ExpeditionId("e-9");

    @Autowired
    private ExpeditionRepository expeditions;
    @Autowired
    private ReplanProposalRepository proposals;
    @Autowired
    private ResourceCatalog catalog;
    @Autowired
    private ZoneRepository zones;
    @Autowired
    private DatabaseCleaner cleaner;

    private final Zone coast = zone("z-1");
    private final Person leader = new Person(new ResourceId("p-1"), "Ada", Set.of());
    private final Person worker = new Person(new ResourceId("p-2"), "Lee", Set.of());
    private final Instrument cooler = new Instrument(new ResourceId("i-1"), "Cooler", COOLER, Set.of());
    private final Depletable jars = new Depletable(new ResourceId("s-1"), "Jars", CONTAINERS, Quantity.of("20", MeasurementUnit.UNITS));

    private static LocalDateTime at(int hour) {
        return LocalDateTime.of(2026, 10, 1, hour, 0);
    }

    @BeforeEach
    void setUp() {
        cleaner.clean();
        zones.add(coast);
        catalog.addStaff(leader);
        catalog.addStaff(worker);
        catalog.addEquipment(cooler);
        catalog.addSupply(jars);
    }

    private Expedition runningExpedition() {
        final var first = sampling("a-1", coast, hours(0, 24));
        final var second = sampling("a-2", coast, hours(0, 24));
        final var night = nightSampling("a-3", coast, hours(0, 24));
        final var dependent = sampling("a-4", coast, hours(0, 24), first);
        final var draft = expeditionIn(coast, leader).build(expeditions);
        draft.schedule(sequential(activity(first, hours(8, 10)), parallel(activity(second, hours(10, 12)), activity(night, hours(20, 22)))));
        draft.schedule(dependent, hours(13, 15));
        final var firstItem = draft.getItinerary().itemFor(first).orElseThrow();
        draft.assignStaff(firstItem, worker);
        draft.assignEquipment(firstItem, cooler);
        draft.assignSupply(firstItem, jars);
        final var running = Expedition.restore(withStatusAndWarning(draft.snapshot()));
        final var item = running.getItinerary().itemFor(first).orElseThrow();
        running.startActivity(item, at(8));
        running.recordObservation(item, new Observation("Calm seas", at(9)));
        running.finishActivity(item, at(10), "Three samples");
        running.registerIncident(running.getItinerary().itemFor(second).orElseThrow(),
                IncidentReport.of("Storm", at(10)).delayingBy(Duration.ofHours(1)));
        return running;
    }

    private ExpeditionSnapshot withStatusAndWarning(ExpeditionSnapshot snapshot) {
        final var warning = ValidationResult.warning(new RuleId("risk-tolerance"), ValidationSubject.of(new ActivityId("a-1")), "Risky");
        return new ExpeditionSnapshot(snapshot.id(), snapshot.name(), snapshot.period(), snapshot.objectives(), snapshot.zones(),
                snapshot.responsibles(), snapshot.restrictions(), snapshot.grantedPermits(), ExpeditionStatus.IN_EXECUTION,
                List.of(new AcceptedWarning(warning, leader, "Both are instructors")), snapshot.incidents(), snapshot.replacedPlan(),
                snapshot.replanReason(), snapshot.replacement(), snapshot.executionRevision(), snapshot.roots());
    }

    @Test
    void anExpeditionComesBackWithItsWholeState() {
        final var expedition = runningExpedition();

        expeditions.add(expedition);

        assertEquals(expedition.snapshot(), expeditions.findById(expedition.getId()).orElseThrow().snapshot());
    }

    @Test
    void anAlternativeKeepsWhatItReplacesItsReasonAndItsPlan() {
        final var original = runningExpedition();
        original.suspend();
        expeditions.add(original);
        final var affected = original.getItinerary().itemFor(new ActivityId("a-4")).orElseThrow();
        final var alternative = new ExpeditionReplanner(expeditions).planAlternative(original, affected, new Delay(Duration.ofHours(1)));

        expeditions.add(alternative);

        final var found = expeditions.findById(alternative.getId()).orElseThrow();
        assertEquals(alternative.snapshot(), found.snapshot());
        assertTrue(found.isBuiltOnCurrentPlanOf(expeditions.findById(original.getId()).orElseThrow()));
    }

    @Test
    void aChangeThatWasNotSavedIsNotPersisted() {
        final var expedition = expeditionIn(coast, leader).build(expeditions);
        expeditions.add(expedition);

        expedition.submitForReview();

        assertEquals(ExpeditionStatus.DRAFT, expeditions.findById(expedition.getId()).orElseThrow().getStatus());
    }

    @Test
    void aProposalComesBackWithEveryKindOfReasonAndItsDecision() {
        final var incident = new Incident(new IncidentId(OWNER, 1), new ActivityId("a-1"), "Storm", at(9), Optional.of(worker.getId()), Optional.of(Duration.ofMinutes(90)));
        final var proposal = new ReplanProposal(proposals.nextId(), OWNER, incident, List.of(
                new ReplanOption(new Cancellation(), "Cancel it"),
                new ReplanOption(new Delay(Duration.ofMinutes(90)), "Delay it"),
                new ReplanOption(new ResourceWithdrawn(worker.getId()), "Withdraw Lee"),
                new ReplanOption(new ResourceReplaced(worker.getId(), ResourceAssignment.of(leader)), "Replace Lee")));
        proposal.accept(4, new ReplanDecision(leader, "Ada knows the site", at(10)), new ExpeditionId("e-10"));

        proposals.add(proposal);

        assertEquals(proposal.snapshot(), proposals.findById(proposal.getId()).orElseThrow().snapshot());
    }

    @Test
    void aVehicleComesBackWithItsPlateAndItsCalendar() {
        final var calendar = new CalendarSnapshot(List.of(new Reservation(OWNER, hours(8, 10))), List.of(new Unavailability(hours(12, 14), "Maintenance")));
        catalog.addEquipment(Vehicle.restored(new ResourceId("v-1"), "Zodiac", new ResourceCategory("Boat"), "AA 123", Set.of(), calendar));

        final var found = assertInstanceOf(Vehicle.class, catalog.findEquipment(new ResourceId("v-1")).orElseThrow());

        assertEquals("AA 123", found.getLicensePlate());
        assertEquals(calendar, found.calendarSnapshot());
    }

    @Test
    void aSupplyComesBackWithItsStockAndItsReservations() {
        jars.reserve(OWNER, Quantity.of("5", MeasurementUnit.UNITS));
        catalog.save(jars);

        final var found = assertInstanceOf(Depletable.class, catalog.findSupply(jars.getId()).orElseThrow());

        assertEquals(Quantity.of("20", MeasurementUnit.UNITS), found.getStock());
        assertEquals(Map.of(OWNER, Quantity.of("5", MeasurementUnit.UNITS)), found.reservations());
        assertFalse(found.hasStockFor(Quantity.of("16", MeasurementUnit.UNITS)));
    }
}
