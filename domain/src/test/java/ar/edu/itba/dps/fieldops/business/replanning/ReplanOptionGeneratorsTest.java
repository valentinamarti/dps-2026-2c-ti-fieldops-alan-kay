package ar.edu.itba.dps.fieldops.business.replanning;

import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentReport;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ResourceAssignment;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanOption;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.resources.Unavailability;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplanOptionGeneratorsTest {

    private static final LocalDateTime TEN = LocalDateTime.of(2026, 10, 1, 10, 0);

    private final Zone coast = zone("z-1");
    private final ResourceCatalog catalog = new ResourceCatalog();
    private final Person sick = person("p-2");
    private final Expedition expedition = expeditionIn(coast, person("p-1")).build(new SequentialExpeditionRepository());
    private final Activity affectedActivity = sampling("a-1", coast, hours(0, 24));
    private final Activity dependentActivity = sampling("a-2", coast, hours(0, 24), affectedActivity);
    private ItineraryItem affected;

    @BeforeEach
    void inExecution() {
        affected = expedition.schedule(affectedActivity, hours(12, 14));
        final var dependent = expedition.schedule(dependentActivity, hours(14, 16));
        expedition.assignStaff(affected, sick);
        expedition.assignEquipment(affected, instrument("c-1", "Cooler"));
        expedition.assignSupply(affected, supply("j-1", "Sample container", of("10", UNITS)));
        expedition.assignStaff(dependent, person("p-9"));
        expedition.assignEquipment(dependent, instrument("c-9", "Cooler"));
        expedition.assignSupply(dependent, supply("j-9", "Sample container", of("10", UNITS)));
        expedition.submitForReview();
        expedition.approve(List.of(), List.of());
        expedition.start();
    }

    @Test
    void reschedulingNeedsTheDelayTheIncidentReports() {
        final var withoutDelay = expedition.registerIncident(affected, IncidentReport.of("Engine failure", TEN));
        final var withDelay = expedition.registerIncident(affected, IncidentReport.of("Engine failure", TEN).delayingBy(Duration.ofMinutes(90)));

        assertEquals(List.of(), new RescheduleOptions().optionsFor(expedition, affected, withoutDelay));
        final var options = new RescheduleOptions().optionsFor(expedition, affected, withDelay);
        assertEquals(List.of(new Delay(Duration.ofMinutes(90))), options.stream().map(ReplanOption::reason).toList());
        assertEquals("Run Sampling a-1 90 min later together with Sampling a-2", options.getFirst().summary());
    }

    @Test
    void cancellingIsAlwaysOfferedAndNamesWhatFallsWithTheActivity() {
        final var incident = expedition.registerIncident(affected, IncidentReport.of("Storm forecast", TEN));

        final var options = new CancellationOptions().optionsFor(expedition, affected, incident);

        assertEquals(new Cancellation(), options.getFirst().reason());
        assertEquals("Cancel Sampling a-1 and what depends on it: Sampling a-2", options.getFirst().summary());
    }

    @Test
    void replacingNeedsTheIncidentToNameAResource() {
        catalog.addStaff(person("p-3"));
        final var incident = expedition.registerIncident(affected, IncidentReport.of("Storm forecast", TEN));

        assertEquals(List.of(), new ResourceReplacementOptions(catalog).optionsFor(expedition, affected, incident));
    }

    @Test
    void offersUpToThreeFreeCandidatesForAPersonInCatalogOrderLeavingOutWhoIsAlreadyThere() {
        catalog.addStaff(sick);
        final var candidates = List.of(person("p-3"), person("p-4"), person("p-5"), person("p-6"));
        candidates.forEach(catalog::addStaff);
        
        final var incident = expedition.registerIncident(affected, IncidentReport.of("Sick", TEN).affecting(sick.getId()));

        final var options = new ResourceReplacementOptions(catalog).optionsFor(expedition, affected, incident);

        assertEquals(ResourceReplacementOptions.MAX_CANDIDATES, options.size());
        assertEquals(candidates.subList(0, 3).stream().map(candidate -> new ResourceReplaced(sick.getId(), ResourceAssignment.of(candidate))).toList(),
                options.stream().map(ReplanOption::reason).toList());
        assertEquals("Replace p-2 with p-3 in Sampling a-1", options.getFirst().summary());
    }

    @Test
    void offersOnlyEquipmentOfTheCategoryTheActivityNeedsThatIsFreeInItsPeriod() {
        final var broken = affected.getAssignedEquipment().iterator().next();
        final var spare = instrument("c-2", "Cooler");
        final var booked = instrument("c-3", "Cooler");
        booked.markUnavailable(new Unavailability(hours(11, 15), "Maintenance"));
        catalog.addEquipment(broken);
        catalog.addEquipment(instrument("b-1", "Boat"));
        catalog.addEquipment(booked);
        catalog.addEquipment(spare);
        final var incident = expedition.registerIncident(affected, IncidentReport.of("Cooler broke", TEN).affecting(broken.getId()));

        final var options = new ResourceReplacementOptions(catalog).optionsFor(expedition, affected, incident);

        assertEquals(List.of(new ResourceReplaced(broken.getId(), ResourceAssignment.of(spare))),
                options.stream().map(ReplanOption::reason).toList());
    }

    @Test
    void offersSuppliesThatCoverTheRequirement() {
        final var broken = affected.getAssignedSupplies().iterator().next();
        final var spare = supply("j-2", "Sample container", of("5", UNITS));
        catalog.addSupply(spare);
        catalog.addSupply(supply("j-3", "Sample container", of("1", UNITS)));
        final var incident = expedition.registerIncident(affected, IncidentReport.of("Jars broke", TEN).affecting(broken.getId()));

        final var options = new ResourceReplacementOptions(catalog).optionsFor(expedition, affected, incident);

        assertEquals(List.of(new ResourceReplaced(broken.getId(), ResourceAssignment.of(spare))),
                options.stream().map(ReplanOption::reason).toList());
    }

    @Test
    void withoutCandidatesThereIsNoReplacementOption() {
        final var incident = expedition.registerIncident(affected, IncidentReport.of("Sick", TEN).affecting(sick.getId()));

        assertTrue(new ResourceReplacementOptions(catalog).optionsFor(expedition, affected, incident).isEmpty());
    }
}
