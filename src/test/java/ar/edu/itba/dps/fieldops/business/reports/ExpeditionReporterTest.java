package ar.edu.itba.dps.fieldops.business.reports;

import ar.edu.itba.dps.fieldops.business.estimations.ExpeditionEstimator;
import ar.edu.itba.dps.fieldops.business.interfaces.validation.ExpeditionValidator;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.resources.Depletable;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.validation.AcceptedWarning;
import ar.edu.itba.dps.fieldops.business.models.validation.ApprovalResult;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.supply;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpeditionReporterTest {

    private static final ResourceCategory SAMPLE_CONTAINER = new ResourceCategory("Sample container");
    private static final ValidationResult RISKY = ValidationResult.warning("Sampling a-1 is risky");

    private final ExpeditionReporter reporter = new ExpeditionReporter(new ExpeditionEstimator());
    private final Zone coast = zone("z-1");
    private final Person leader = person("p-1");
    private final Expedition expedition = expeditionIn(coast, leader).build();
    private final Depletable jars = supply("d-1", "Sample container", Quantity.of("20", UNITS));
    private final ItineraryItem morning = expedition.schedule(sampling("a-1", coast, hours(0, 24)), hours(9, 11));
    private final ItineraryItem afternoon = expedition.schedule(sampling("a-2", coast, hours(0, 24)), hours(14, 16));

    private static ExpeditionValidator finding(ValidationResult... results) {
        return expedition -> new ApprovalResult(List.of(results));
    }

    private static LocalDateTime at(int hour) {
        return LocalDateTime.of(2026, 10, 1, hour, 0);
    }

    private void execute(List<AcceptedWarning> acceptances, ValidationResult... findings) {
        expedition.assignStaff(morning, person("p-2"));
        expedition.assignSupply(morning, jars);
        expedition.assignStaff(afternoon, person("p-3"));
        expedition.assignSupply(afternoon, jars);
        expedition.submitForReview();
        expedition.approve(finding(findings), acceptances);
        expedition.start();
    }

    @Test
    void reportsAPlanThatHasNotBeenExecutedYet() {
        final var report = reporter.reportOf(expedition);

        assertEquals("Coastal survey", report.expeditionName());
        assertEquals(ExpeditionStatus.DRAFT, report.status());
        assertEquals(2, report.scheduledActivities());
        assertEquals(0, report.finishedActivities());
        assertTrue(report.actualConsumption().isEmpty());
        assertTrue(report.incidents().isEmpty());
        assertFalse(report.isComplete());
    }

    @Test
    void reusesTheEstimatorForTheExpectedDurationRiskAndConsumption() {
        final var report = reporter.reportOf(expedition);

        assertEquals(new ExpeditionEstimator().estimate(expedition), report.estimate());
        assertEquals(RiskLevel.LOW, report.estimate().overallRisk());
        assertEquals(Map.of(SAMPLE_CONTAINER, Quantity.of("6", UNITS)), report.estimate().estimatedConsumption());
    }

    @Test
    void countsOnlyWhatWasActuallyConsumedByFinishedActivities() {
        execute(List.of());
        expedition.startActivity(morning, at(9));
        expedition.finishActivity(morning, at(11), "Six samples collected");

        final var report = reporter.reportOf(expedition);

        assertEquals(1, report.finishedActivities());
        assertEquals(Map.of(SAMPLE_CONTAINER, Quantity.of("3", UNITS)), report.actualConsumption());
        assertFalse(report.isComplete());
    }

    @Test
    void gathersTheIncidentsOfEveryActivity() {
        execute(List.of());
        expedition.startActivity(morning, at(9));
        expedition.startActivity(afternoon, at(14));
        expedition.reportIncident(morning, new Incident("One jar broke", at(10)));
        expedition.reportIncident(afternoon, new Incident("The boat engine failed", at(15)));

        final var report = reporter.reportOf(expedition);

        assertEquals(List.of(new Incident("One jar broke", at(10)), new Incident("The boat engine failed", at(15))),
                report.incidents());
    }

    @Test
    void carriesTheWarningsAResponsibleAcceptedToApproveIt() {
        final var acceptance = new AcceptedWarning(RISKY, leader, "The team is experienced");
        execute(List.of(acceptance), RISKY);

        assertEquals(List.of(acceptance), reporter.reportOf(expedition).acceptedWarnings());
    }

    @Test
    void anExpeditionWithEveryActivityFinishedIsComplete() {
        execute(List.of());
        expedition.startActivity(morning, at(9));
        expedition.finishActivity(morning, at(11), "Done");
        expedition.startActivity(afternoon, at(14));
        expedition.finishActivity(afternoon, at(16), "Done");
        expedition.finish();

        final var report = reporter.reportOf(expedition);

        assertEquals(ExpeditionStatus.FINISHED, report.status());
        assertEquals(Map.of(SAMPLE_CONTAINER, Quantity.of("6", UNITS)), report.actualConsumption());
        assertTrue(report.isComplete());
    }
}
