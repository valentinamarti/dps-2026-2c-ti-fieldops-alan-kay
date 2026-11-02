package ar.edu.itba.dps.fieldops.business.rules;

import ar.edu.itba.dps.fieldops.business.interfaces.activities.ActivityRules;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityDefinition;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActivityDefinitionTest {
    private final ActivityTypeRegistry registry = ActivityTypeRegistry.withStandardTypes();

    static Stream<Arguments> everyType() {
        return Stream.of(
                Arguments.of(new SampleCollectionRules(3), "sample collection", "sampleCount", 3),
                Arguments.of(new DivingRules(10), "diving", "maxDepthMeters", 10),
                Arguments.of(new DroneSurveyRules(20), "drone survey", "areaHectares", 20),
                Arguments.of(new WildlifeObservationRules(2), "wildlife observation", "observationPoints", 2),
                Arguments.of(new WaterQualityMeasurementRules(4), "water quality measurement", "stationCount", 4),
                Arguments.of(new TransferRules(60), "transfer", "distanceKilometers", 60));
    }

    private void assertRebuilds(ActivityRules rules, ActivityDefinition definition) {
        final var rebuilt = registry.rulesFor(definition);

        assertEquals(definition, rebuilt.definition());
        assertEquals(rules.estimatedDuration(), rebuilt.estimatedDuration());
        assertEquals(rules.estimatedRisk(), rebuilt.estimatedRisk());
        assertEquals(rules.requiredEquipment(), rebuilt.requiredEquipment());
        assertEquals(rules.requiredSupplies(), rebuilt.requiredSupplies());
        assertEquals(rules.requiredStaff(), rebuilt.requiredStaff());
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("everyType")
    void theRulesOfEachTypeAreRebuiltFromTheirDefinition(ActivityRules rules, String type, String parameter, int value) {
        final var definition = rules.definition();

        assertEquals(new ActivityDefinition(type, Map.of(parameter, value), false), definition);
        assertRebuilds(rules, definition);
    }

    @ParameterizedTest(name = "{1} at night")
    @MethodSource("everyType")
    void aNightOperationKeepsItsVariantInTheDefinition(ActivityRules rules, String type, String parameter, int value) {
        final var night = new NightOperationRules(rules);

        assertEquals(new ActivityDefinition(type, Map.of(parameter, value), true), night.definition());
        assertRebuilds(night, night.definition());
    }
}
