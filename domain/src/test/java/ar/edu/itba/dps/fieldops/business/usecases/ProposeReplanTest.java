package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentReport;
import ar.edu.itba.dps.fieldops.business.models.replanning.ReplanOption;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.resources.Unavailability;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.usecases.IncidentScenario.TEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProposeReplanTest {
    private static final String RESCHEDULE = "Run";
    private static final String REPLACE = "Replace";
    private static final String CANCEL = "Cancel";

    private static Arguments offers(String what, Function<IncidentScenario, Activity> affected,
                                    UnaryOperator<IncidentReport> details, boolean freeStaffInCatalog, List<String> kinds) {
        return Arguments.of(what, affected, details, freeStaffInCatalog, kinds);
    }

    static Stream<Arguments> incidents() {
        return Stream.of(
                offers("only a description", s -> s.afternoon, report -> report, false, List.of(CANCEL)),
                offers("a delay that fits", s -> s.afternoon, report -> report.delayingBy(Duration.ofHours(1)), false, List.of(RESCHEDULE, CANCEL)),
                offers("a delay that pushes a dependent out of its window", s -> s.afternoon,
                        report -> report.delayingBy(Duration.ofHours(2)), false, List.of(CANCEL)),
                offers("a resource with free candidates", s -> s.morning,
                        report -> report.affecting(new ResourceId("s-a-1")), true, List.of(REPLACE, REPLACE, REPLACE, CANCEL)),
                offers("a resource without candidates", s -> s.morning,
                        report -> report.affecting(new ResourceId("s-a-1")), false, List.of(CANCEL)),
                offers("a resource and a delay", s -> s.morning,
                        report -> report.affecting(new ResourceId("s-a-1")).delayingBy(Duration.ofHours(1)),
                        true, List.of(RESCHEDULE, REPLACE, REPLACE, REPLACE, CANCEL)));
    }

    private static void prepareCatalog(IncidentScenario scenario, boolean freeStaffInCatalog) {
        if (freeStaffInCatalog) {
            scenario.catalog.addStaff(person("p-5"));
        } else {
            scenario.catalog.allStaff().forEach(person -> person.markUnavailable(new Unavailability(hours(0, 24), "Leave")));
        }
    }

    @ParameterizedTest(name = "an incident with {0}")
    @MethodSource("incidents")
    void offersTheOptionsThatMakeSenseAndCanBeBuilt(String what, Function<IncidentScenario, Activity> affected,
                                                     UnaryOperator<IncidentReport> details, boolean freeStaffInCatalog,
                                                     List<String> kinds) {
        final var scenario = new IncidentScenario();
        prepareCatalog(scenario, freeStaffInCatalog);

        final var proposal = scenario.registerIncident.register(
                scenario.incidentOn(affected.apply(scenario), details.apply(IncidentReport.of("Something went wrong", TEN))));

        assertEquals(kinds, proposal.getOptions().stream().map(ReplanOption::summary).map(summary -> summary.split(" ")[0]).toList());
    }

    @ParameterizedTest(name = "an incident with {0}")
    @MethodSource("incidents")
    void neverOffersAnOptionThatCannotBeAccepted(String what, Function<IncidentScenario, Activity> affected,
                                                  UnaryOperator<IncidentReport> details, boolean freeStaffInCatalog,
                                                  List<String> kinds) {
        final var scenario = new IncidentScenario();
        prepareCatalog(scenario, freeStaffInCatalog);
        final var activity = affected.apply(scenario);
        final var proposal = scenario.registerIncident.register(
                scenario.incidentOn(activity, details.apply(IncidentReport.of("Something went wrong", TEN))));
        final var item = scenario.itemOf(scenario.expedition, activity);

        proposal.getOptions().forEach(option ->
                assertTrue(scenario.replanner.canPlan(scenario.expedition, item, option.reason()), option.summary()));
    }
}
