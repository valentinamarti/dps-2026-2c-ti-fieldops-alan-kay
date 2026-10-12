package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.estimations.ExpeditionEstimator;
import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.estimation.ExpeditionEstimate;
import ar.edu.itba.dps.fieldops.business.models.report.ExpeditionReport;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationSubject;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.reports.ExpeditionReporter;
import ar.edu.itba.dps.fieldops.business.validations.ApprovalPolicy;
import ar.edu.itba.dps.fieldops.business.validations.ResourceOverlapValidation;
import ar.edu.itba.dps.fieldops.business.validations.RiskToleranceValidation;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.DIVING;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.completeAssignments;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.diving;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.instrument;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.supply;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.vehicle;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.LITERS;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItineraryWithoutBlocksRegressionTest {

    private static final ResourceCategory SAMPLE_CONTAINER = new ResourceCategory("Sample container");
    private static final ResourceCategory FUEL = new ResourceCategory("Fuel");
    private static final ExpeditionEstimate ESTIMATE_BEFORE_BLOCKS = new ExpeditionEstimate(
            Duration.ofMinutes(120 + 120 + 125),
            RiskLevel.HIGH,
            Map.of(SAMPLE_CONTAINER, Quantity.of("6", UNITS), FUEL, Quantity.of("20", LITERS)));

    private final SequentialExpeditionRepository repository = new SequentialExpeditionRepository();
    private final Zone coast = zone("z-1");
    private final Expedition expedition = expeditionIn(coast, person("p-1")).build(repository);
    private final Person sharedWorker = person("p-2");
    private final ItineraryItem lateSampling = expedition.schedule(sampling("a-2", coast, hours(0, 24)), hours(10, 12));
    private final ItineraryItem earlySampling = expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(9, 11));
    private final ItineraryItem deepDive = expedition.schedule(diving("a-3", coast, hours(0, 24), 40), hours(14, 17));

    ItineraryWithoutBlocksRegressionTest() {
        expedition.assignStaff(earlySampling, sharedWorker);
        expedition.assignStaff(lateSampling, sharedWorker);
        expedition.assignStaff(deepDive, person("p-3", DIVING));
        expedition.assignStaff(deepDive, person("p-4", DIVING));
        expedition.assignEquipment(deepDive, vehicle("v-1", "Boat"));
        expedition.assignEquipment(deepDive, instrument("i-1", "Oxygen tank"));
        expedition.assignEquipment(deepDive, instrument("i-2", "Oxygen tank"));
        expedition.assignSupply(deepDive, supply("d-1", "Fuel", Quantity.of("50", LITERS)));
        completeAssignments(expedition);
    }

    @Test
    void anItineraryWithoutBlocksListsEveryActivityAsItsOwnNodeOrderedByStart() {
        final var itinerary = expedition.getItinerary();

        assertEquals(List.of(earlySampling, lateSampling, deepDive), itinerary.getItems());
        assertEquals(List.of(earlySampling, lateSampling, deepDive), itinerary.getNodes());
        assertTrue(itinerary.getItems().stream().noneMatch(item -> itinerary.runInParallel(item, earlySampling)));
    }

    @Test
    void anItineraryWithoutBlocksValidatesAsBefore() {
        final var overlap = ValidationResult.critical(ResourceOverlapValidation.ID, ValidationSubject.of(new ActivityId("a-1"), new ResourceId("p-2")), "p-2 is assigned to Sampling a-1 and Sampling a-2 at the same time");
        final var risk = ValidationResult.warning(RiskToleranceValidation.ID, ValidationSubject.of(new ActivityId("a-3")), "Diving a-3 has HIGH risk, above the expedition tolerance of MEDIUM");

        assertEquals(List.of(overlap, risk), new ApprovalPolicy(List.of()).validate(expedition).results());
    }

    @Test
    void anItineraryWithoutBlocksEstimatesAsBefore() {
        assertEquals(ESTIMATE_BEFORE_BLOCKS, new ExpeditionEstimator().estimate(expedition));
    }

    @Test
    void anItineraryWithoutBlocksReportsAsBefore() {
        final var expected = new ExpeditionReport("Coastal survey", ExpeditionStatus.DRAFT, 3, 0, ESTIMATE_BEFORE_BLOCKS,
                Map.of(), List.of(), List.of());

        assertEquals(expected, new ExpeditionReporter(new ExpeditionEstimator()).reportOf(expedition));
    }
}
