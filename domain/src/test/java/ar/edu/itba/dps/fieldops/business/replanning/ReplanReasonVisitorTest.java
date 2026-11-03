package ar.edu.itba.dps.fieldops.business.replanning;

import ar.edu.itba.dps.fieldops.business.interfaces.replanning.ReplanReason;
import ar.edu.itba.dps.fieldops.business.interfaces.replanning.ReplanReasonVisitor;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ResourceAssignment;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.stream.Stream;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ReplanReasonVisitorTest {
    private static final ResourceId SICK = new ResourceId("p-2");

    private static final ReplanReasonVisitor<String> NAME = new ReplanReasonVisitor<>() {
        @Override
        public String cancellation() {
            return "cancellation";
        }

        @Override
        public String delay(Duration delay) {
            return "delay " + delay.toMinutes();
        }

        @Override
        public String resourceWithdrawn(ResourceId resource) {
            return "withdrawn " + resource;
        }

        @Override
        public String resourceReplaced(ResourceId withdrawn, ResourceAssignment replacement) {
            return "replaced " + withdrawn + " with " + replacement;
        }
    };

    static Stream<Arguments> reasons() {
        return Stream.of(
                Arguments.of(new Cancellation(), "cancellation"),
                Arguments.of(new Delay(Duration.ofHours(1)), "delay 60"),
                Arguments.of(new ResourceWithdrawn(SICK), "withdrawn p-2"),
                Arguments.of(new ResourceReplaced(SICK, ResourceAssignment.of(person("p-3"))), "replaced p-2 with p-3"));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("reasons")
    void eachReasonTellsWhatItIs(ReplanReason reason, String expected) {
        assertEquals(expected, reason.accept(NAME));
    }
}
