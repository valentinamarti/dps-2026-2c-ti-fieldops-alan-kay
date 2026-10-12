package ar.edu.itba.dps.fieldops.business.rules;

import ar.edu.itba.dps.fieldops.business.exceptions.InvalidActivityParametersException;
import ar.edu.itba.dps.fieldops.business.exceptions.UnknownActivityTypeException;
import ar.edu.itba.dps.fieldops.business.interfaces.activities.ActivityRules;
import ar.edu.itba.dps.fieldops.business.interfaces.activities.ActivityType;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityTypeName;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActivityTypeRegistryTest {

    private static final Zone REEF = new Zone("z-1", "Reef", Set.of());

    private final ActivityTypeRegistry registry = ActivityTypeRegistry.withStandardTypes();

    private static Stream<Arguments> everyTypeWithItsParameter() {
        return Stream.of(
                Arguments.of("sample collection", "sampleCount", 3, new SampleCollectionRules(3)),
                Arguments.of("diving", "maxDepthMeters", 40, new DivingRules(40)),
                Arguments.of("drone survey", "areaHectares", 20, new DroneSurveyRules(20)),
                Arguments.of("wildlife observation", "observationPoints", 2, new WildlifeObservationRules(2)),
                Arguments.of("water quality measurement", "stationCount", 4, new WaterQualityMeasurementRules(4)),
                Arguments.of("transfer", "distanceKilometers", 60, new TransferRules(60)));
    }

    private static void assertSameRules(ActivityRules expected, ActivityRules actual) {
        assertEquals(expected.estimatedDuration(), actual.estimatedDuration());
        assertEquals(expected.estimatedRisk(), actual.estimatedRisk());
        assertEquals(expected.requiredEquipment(), actual.requiredEquipment());
        assertEquals(expected.requiredSupplies(), actual.requiredSupplies());
        assertEquals(expected.requiredStaff(), actual.requiredStaff());
        assertEquals(expected.requiredCertificationsForAllStaff(), actual.requiredCertificationsForAllStaff());
        assertEquals(expected.requiredPermitsIn(REEF), actual.requiredPermitsIn(REEF));
    }

    @ParameterizedTest(name = "{0} with {1} = {2}")
    @MethodSource("everyTypeWithItsParameter")
    void createsTheRulesOfEachTypeFromItsNameAndItsParameter(String type, String parameter, int value, ActivityRules expected) {
        assertSameRules(expected, registry.rulesFor(type, Map.of(parameter, value), false));
    }

    @ParameterizedTest(name = "a night {0}")
    @MethodSource("everyTypeWithItsParameter")
    void wrapsTheBaseRulesOfTheTypeWhenTheNightVariantIsAskedFor(String type, String parameter, int value, ActivityRules base) {
        assertSameRules(new NightOperationRules(base), registry.rulesFor(type, Map.of(parameter, value), true));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"diving", "Diving", "DIVING", "  diving "})
    void findsATypeWhateverTheCaseAndTheSpacesAroundItsName(String type) {
        assertSameRules(new DivingRules(10), registry.rulesFor(type, Map.of("maxDepthMeters", 10), false));
    }

    @Test
    void rejectsAnUnknownTypeNamingTheAvailableOnes() {
        final var failure = assertThrows(UnknownActivityTypeException.class,
                () -> registry.rulesFor("Spelunking", Map.of("depth", 10), false));

        assertEquals("There is no activity type named spelunking. The available types are: sample collection, diving, "
                + "drone survey, wildlife observation, water quality measurement, transfer", failure.getMessage());
    }

    @ParameterizedTest(name = "{0} without {1}")
    @MethodSource("everyTypeWithItsParameter")
    void rejectsATypeWithoutItsParameter(String type, String parameter) {
        final var failure = assertThrows(InvalidActivityParametersException.class, () -> registry.rulesFor(type, Map.of(), false));

        assertEquals("An activity of type %s cannot be created: the parameter %s is required".formatted(type, parameter),
                failure.getMessage());
    }

    @ParameterizedTest(name = "{0} with {1} = 0")
    @MethodSource("everyTypeWithItsParameter")
    void rejectsATypeWithAnInvalidParameter(String type, String parameter) {
        final var failure = assertThrows(InvalidActivityParametersException.class,
                () -> registry.rulesFor(type, Map.of(parameter, 0), false));

        assertTrue(failure.getMessage().startsWith("An activity of type %s cannot be created: %s cannot be 0, ".formatted(type, parameter)));
    }

    @Test
    void rejectsANegativeParameterSayingWhatTheTypeExpects() {
        final var failure = assertThrows(InvalidActivityParametersException.class,
                () -> registry.rulesFor("diving", Map.of("maxDepthMeters", -5), true));

        assertEquals("An activity of type diving cannot be created: maxDepthMeters cannot be -5, max depth must be positive",
                failure.getMessage());
    }

    @Test
    void aParameterWithoutValueOrWithAnotherNameCountsAsMissing() {
        final var withoutValue = new HashMap<String, Integer>();
        withoutValue.put("maxDepthMeters", null);

        assertThrows(InvalidActivityParametersException.class, () -> registry.rulesFor("diving", withoutValue, false));
        assertThrows(InvalidActivityParametersException.class, () -> registry.rulesFor("diving", Map.of("maxdepthmeters", 10), false));
        assertThrows(InvalidActivityParametersException.class, () -> registry.rulesFor("diving", Map.of("sampleCount", 3), false));
    }

    @Test
    void rejectsABlankTypeNameAndMissingParameters() {
        assertThrows(IllegalArgumentException.class, () -> registry.rulesFor(" ", Map.of(), false));
        assertThrows(NullPointerException.class, () -> registry.rulesFor("diving", null, false));
    }

    @Test
    void listsTheSixStandardTypesWithTheParameterEachOneTakes() {
        final var parametersByType = registry.types().stream()
                .collect(Collectors.toMap(type -> type.name().value(), ActivityType::parameterNames));

        assertEquals(Map.of(
                "sample collection", List.of("sampleCount"),
                "diving", List.of("maxDepthMeters"),
                "drone survey", List.of("areaHectares"),
                "wildlife observation", List.of("observationPoints"),
                "water quality measurement", List.of("stationCount"),
                "transfer", List.of("distanceKilometers")), parametersByType);
    }

    @Test
    void aRegistryOnlyKnowsTheTypesItWasGiven() {
        final var onlyDiving = new ActivityTypeRegistry(List.of(DivingRules.TYPE));

        assertEquals(List.of(new ActivityTypeName("Diving")), onlyDiving.types().stream().map(ActivityType::name).toList());
        assertThrows(UnknownActivityTypeException.class, () -> onlyDiving.rulesFor("transfer", Map.of("distanceKilometers", 60), false));
    }

    @Test
    void rejectsTwoTypesWithTheSameName() {
        assertThrows(IllegalArgumentException.class, () -> new ActivityTypeRegistry(List.of(DivingRules.TYPE, DivingRules.TYPE)));
    }
}
