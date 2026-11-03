package ar.edu.itba.dps.fieldops.business.models;

import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentId;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposal;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanProposalId;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.instrument;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.supply;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.vehicle;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;
import static ar.edu.itba.dps.fieldops.business.models.common.Quantity.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class EntityIdentityTest {

    private static ReplanProposal proposal(String id) {
        final var original = new ExpeditionId("e-1");
        final var incident = new Incident(new IncidentId(original, 1), new ActivityId("a-1"), "Storm",
                LocalDateTime.of(2026, 10, 1, 9, 0), Optional.empty(), Optional.empty());
        return new ReplanProposal(new ReplanProposalId(id), original, incident, List.of());
    }

    static Stream<Arguments> entities() {
        return Stream.of(
                Arguments.of("person", (Function<String, Object>) id -> person(id)),
                Arguments.of("vehicle", (Function<String, Object>) id -> vehicle(id, "Boat")),
                Arguments.of("instrument", (Function<String, Object>) id -> instrument(id, "Cooler")),
                Arguments.of("supply", (Function<String, Object>) id -> supply(id, "Fuel", of("10", UNITS))),
                Arguments.of("zone", (Function<String, Object>) id -> zone(id)),
                Arguments.of("activity", (Function<String, Object>) id -> sampling(id, zone("z-1"), hours(0, 24))),
                Arguments.of("proposal", (Function<String, Object>) EntityIdentityTest::proposal),
                Arguments.of("expedition", (Function<String, Object>) id -> expeditionIn(zone("z-1"), person("p-1")).build(() -> new ExpeditionId(id))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("entities")
    void twoInstancesWithTheSameIdAreTheSameEntity(String kind, Function<String, Object> entity) {
        assertEquals(entity.apply("x-1"), entity.apply("x-1"));
        assertEquals(entity.apply("x-1").hashCode(), entity.apply("x-1").hashCode());
        assertNotEquals(entity.apply("x-1"), entity.apply("x-2"));
    }
}
