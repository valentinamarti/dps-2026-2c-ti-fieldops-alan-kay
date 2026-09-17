package ar.edu.itba.dps.fieldops.business.estimations;

import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.diving;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.LITERS;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpeditionEstimatorTest {

    private static final ResourceCategory SAMPLE_CONTAINER = new ResourceCategory("Sample container");
    private static final ResourceCategory FUEL = new ResourceCategory("Fuel");

    private final ExpeditionEstimator estimator = new ExpeditionEstimator();
    private final Zone coast = zone("z-1");
    private final Expedition expedition = expeditionIn(coast, person("p-1")).build();

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
}
