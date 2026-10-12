package ar.edu.itba.dps.fieldops.business.estimations;

import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.diving;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.LITERS;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.activity;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.parallel;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.sequential;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpeditionEstimatorTest {

    private static final ResourceCategory SAMPLE_CONTAINER = new ResourceCategory("Sample container");
    private static final ResourceCategory FUEL = new ResourceCategory("Fuel");

    private final SequentialExpeditionRepository repository = new SequentialExpeditionRepository();
    private final ExpeditionEstimator estimator = new ExpeditionEstimator();
    private final Zone coast = zone("z-1");
    private final Expedition expedition = expeditionIn(coast, person("p-1")).build(repository);

    @Test
    void anEmptyItineraryTakesNoTimeAndCarriesNoRisk() {
        final var estimate = estimator.estimate(expedition);

        assertEquals(Duration.ZERO, estimate.totalDuration());
        assertEquals(RiskLevel.LOW, estimate.overallRisk());
        assertTrue(estimate.estimatedConsumption().isEmpty());
    }

    @Test
    void addsUpTheDurationEveryActivityEstimates() {
        expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(8, 11));
        expedition.schedule(diving("a-2", coast, hours(0, 24), 10), hours(12, 14));

        assertEquals(Duration.ofMinutes(185), estimator.estimate(expedition).totalDuration());
    }

    @Test
    void theRiskOfTheExpeditionIsTheRiskOfItsRiskiestActivity() {
        expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(8, 11));
        expedition.schedule(diving("a-2", coast, hours(0, 24), 40), hours(12, 14));

        assertEquals(RiskLevel.HIGH, estimator.estimate(expedition).overallRisk());
    }

    @Test
    void addsUpTheConsumptionOfEachSupplyCategory() {
        expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(8, 11));
        expedition.schedule(sampling("a-2", coast, hours(0, 24)), hours(12, 14));
        expedition.schedule(diving("a-3", coast, hours(0, 24), 10), hours(15, 17));

        final var expected = Map.of(
                SAMPLE_CONTAINER, Quantity.of("6", UNITS),
                FUEL, Quantity.of("20", LITERS));
        assertEquals(expected, estimator.estimate(expedition).estimatedConsumption());
    }

    @Test
    void estimatingDoesNotDependOnTheExpeditionBeingApproved() {
        expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(8, 11));

        assertEquals(Duration.ofHours(2), estimator.estimate(expedition).totalDuration());
    }

    private static Stream<Arguments> blocksAndTheTimeTheyTake() {
        return Stream.of(
                block("a sequential block", coast -> sequential(
                        activity(sampling("a-1", coast, hours(0, 24)), hours(8, 10)),
                        activity(diving("a-2", coast, hours(0, 24), 10), hours(10, 12))), Duration.ofMinutes(185)),
                block("a parallel block", coast -> parallel(
                        activity(sampling("a-1", coast, hours(0, 24)), hours(8, 10)),
                        activity(diving("a-2", coast, hours(0, 24), 10), hours(8, 10))), Duration.ofMinutes(120)),
                block("a parallel block inside a sequential block", coast -> sequential(
                        activity(sampling("a-1", coast, hours(0, 24)), hours(8, 10)),
                        parallel(
                                activity(sampling("a-2", coast, hours(0, 24)), hours(10, 12)),
                                activity(diving("a-3", coast, hours(0, 24), 10), hours(10, 12)))), Duration.ofMinutes(240)),
                block("a sequential block inside a parallel block", coast -> parallel(
                        activity(sampling("a-1", coast, hours(0, 24)), hours(8, 10)),
                        sequential(
                                activity(diving("a-2", coast, hours(0, 24), 10), hours(8, 10)),
                                activity(diving("a-3", coast, hours(0, 24), 20), hours(10, 12)))), Duration.ofMinutes(150)));
    }

    private static Arguments block(String name, Function<Zone, PlannedNode> plan, Duration expected) {
        return Arguments.of(name, plan, expected);
    }

    @ParameterizedTest(name = "{0} takes {2}")
    @MethodSource("blocksAndTheTimeTheyTake")
    void theDurationOfABlockDependsOnWhetherItsPartsRunInSequenceOrInParallel(String name, Function<Zone, PlannedNode> plan,
                                                                              Duration expected) {
        final var block = expedition.schedule(plan.apply(coast));

        assertEquals(expected, block.estimatedDuration());
        assertEquals(expected, estimator.estimate(expedition).totalDuration());
    }

    @Test
    void theDurationOfASequentialBlockIsTheSumOfItsPartsAndTheOneOfAParallelBlockIsTheMaximum() {
        final var inSequence = expedition.schedule(sequential(
                activity(sampling("a-1", coast, hours(0, 24)), hours(8, 10)),
                activity(diving("a-2", coast, hours(0, 24), 10), hours(10, 12))));
        final var inParallel = expedition.schedule(parallel(
                activity(sampling("a-3", coast, hours(0, 24)), hours(13, 15)),
                activity(diving("a-4", coast, hours(0, 24), 10), hours(13, 15))));

        assertEquals(Duration.ofMinutes(120 + 65), inSequence.estimatedDuration());
        assertEquals(Duration.ofMinutes(120), inParallel.estimatedDuration());
    }

    @Test
    void theTotalDurationAddsUpBlocksAndLooseActivities() {
        expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(8, 10));
        expedition.schedule(parallel(
                activity(sampling("a-2", coast, hours(0, 24)), hours(10, 12)),
                activity(diving("a-3", coast, hours(0, 24), 10), hours(10, 12))));

        assertEquals(Duration.ofMinutes(120 + 120), estimator.estimate(expedition).totalDuration());
    }

    @Test
    void aBlockCarriesTheHighestRiskAndAddsUpTheConsumptionOfItsParts() {
        final var block = expedition.schedule(parallel(
                activity(sampling("a-1", coast, hours(0, 24)), hours(8, 10)),
                sequential(
                        activity(sampling("a-2", coast, hours(0, 24)), hours(8, 10)),
                        activity(diving("a-3", coast, hours(0, 24), 40), hours(10, 13)))));

        final var estimate = estimator.estimate(expedition);

        assertEquals(RiskLevel.HIGH, block.estimatedRisk());
        assertEquals(RiskLevel.HIGH, estimate.overallRisk());
        assertEquals(Map.of(SAMPLE_CONTAINER, Quantity.of("6", UNITS), FUEL, Quantity.of("20", LITERS)),
                estimate.estimatedConsumption());
    }
}
