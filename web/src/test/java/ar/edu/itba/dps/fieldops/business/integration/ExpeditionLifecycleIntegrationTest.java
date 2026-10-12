package ar.edu.itba.dps.fieldops.business.integration;

import ar.edu.itba.dps.fieldops.business.exceptions.ExpeditionNotApprovableException;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionBuilder;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionRestrictions;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentReport;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.resources.Depletable;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.validation.AcceptedWarning;
import ar.edu.itba.dps.fieldops.business.models.validation.Severity;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.replanning.Delay;
import ar.edu.itba.dps.fieldops.business.reports.ExpeditionReporter;
import ar.edu.itba.dps.fieldops.business.usecases.ApproveExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.CreateExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.FinishExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.ReplanExpedition;
import ar.edu.itba.dps.fieldops.business.validations.ApprovalPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.DIVING;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.diving;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.instrument;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.supply;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.vehicle;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.LITERS;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ExpeditionLifecycleIntegrationTest {

    private static final Permit MARINE_RESERVE = new Permit("Marine reserve access");
    private static final ResourceCategory FUEL = new ResourceCategory("Fuel");
    private static final ResourceCategory SAMPLE_CONTAINER = new ResourceCategory("Sample container");

    private final CreateExpedition createExpedition;
    private final ApproveExpedition approveExpedition;
    private final ReplanExpedition replanExpedition;
    private final FinishExpedition finishExpedition;
    private final ExpeditionReporter reporter;
    private final ApprovalPolicy preview = new ApprovalPolicy(List.of());

    private final Zone reef = new Zone("z-1", "Protected reef", Set.of(MARINE_RESERVE));
    private final Person leader = person("p-1", DIVING);
    private final Depletable fuel = supply("d-1", "Fuel", Quantity.of("100", LITERS));
    private final Depletable jars = supply("d-2", "Sample container", Quantity.of("20", UNITS));
    private final ResourceCatalog catalog = new ResourceCatalog();

    private final TimePeriod divePeriod = hours(9, 12);
    private final TimePeriod samplingPeriod = hours(13, 15);
    private final Activity dive = diving("a-1", reef, hours(8, 18), 40);
    private final Activity survey = sampling("a-2", reef, hours(8, 18));

    @Autowired
    ExpeditionLifecycleIntegrationTest(CreateExpedition createExpedition,
                                       ApproveExpedition approveExpedition,
                                       ReplanExpedition replanExpedition,
                                       FinishExpedition finishExpedition,
                                       ExpeditionReporter reporter) {
        this.createExpedition = createExpedition;
        this.approveExpedition = approveExpedition;
        this.replanExpedition = replanExpedition;
        this.finishExpedition = finishExpedition;
        this.reporter = reporter;
        catalog.addStaff(leader);
        catalog.addStaff(person("p-2", DIVING));
        catalog.addStaff(person("p-3"));
        catalog.addEquipment(vehicle("v-1", "Boat"));
        catalog.addEquipment(instrument("i-1", "Oxygen tank"));
        catalog.addEquipment(instrument("i-2", "Oxygen tank"));
        catalog.addEquipment(instrument("i-3", "Cooler"));
        catalog.addSupply(fuel);
        catalog.addSupply(jars);
    }

    private ExpeditionBuilder reefSurvey() {
        return new ExpeditionBuilder()
                .withName("Reef survey")
                .withPeriod(hours(0, 24))
                .addObjective("Map the reef and collect water samples")
                .addZone(reef)
                .addResponsible(leader)
                .addPermit(MARINE_RESERVE)
                .withRestrictions(new ExpeditionRestrictions(10, RiskLevel.MEDIUM));
    }

    private void assignFromCatalog(Expedition expedition, ItineraryItem item) {
        final var period = item.getScheduledPeriod();
        item.getActivity().requiredStaff().forEach(requirement -> catalog.availableStaffFor(requirement, period).stream()
                .limit(requirement.headcount())
                .forEach(person -> expedition.assignStaff(item, person)));
        item.getActivity().requiredEquipment().forEach(requirement -> catalog.availableEquipmentFor(requirement, period).stream()
                .limit(requirement.count())
                .forEach(equipment -> expedition.assignEquipment(item, equipment)));
        item.getActivity().requiredSupplies().forEach(requirement -> catalog.suppliesCovering(requirement).stream()
                .findFirst()
                .ifPresent(supply -> expedition.assignSupply(item, supply)));
    }

    private Expedition approvedReefSurvey() {
        final var expedition = createExpedition.create(reefSurvey());
        assignFromCatalog(expedition, expedition.schedule(dive, divePeriod));
        assignFromCatalog(expedition, expedition.schedule(survey, samplingPeriod));
        final var riskyDive = preview.validate(expedition).warnings().getFirst();
        expedition.submitForReview();
        approveExpedition.approve(expedition, List.of(new AcceptedWarning(riskyDive, leader, "Both divers are instructors")));
        return expedition;
    }

    private ItineraryItem itemOf(Expedition expedition, Activity activity) {
        return expedition.getItinerary().itemFor(activity).orElseThrow();
    }

    @Test
    void planningApprovingAndExecutingAnExpeditionEndToEnd() {
        final var expedition = createExpedition.create(reefSurvey());
        final var diveItem = expedition.schedule(dive, divePeriod);
        final var surveyItem = expedition.schedule(survey, samplingPeriod);
        assignFromCatalog(expedition, diveItem);
        assignFromCatalog(expedition, surveyItem);

        final var result = preview.validate(expedition);
        assertTrue(result.criticals().isEmpty(), () -> "unexpected criticals: " + result.criticals());
        assertEquals(1, result.warnings().size());
        assertEquals(Severity.WARNING, result.warnings().getFirst().severity());

        expedition.submitForReview();
        final var acceptance = new AcceptedWarning(result.warnings().getFirst(), leader, "Both divers are instructors");
        approveExpedition.approve(expedition, List.of(acceptance));
        assertEquals(ExpeditionStatus.APPROVED, expedition.getStatus());
        assertFalse(leader.isAvailableDuring(divePeriod));

        expedition.start();
        expedition.startActivity(diveItem, LocalDateTime.of(2026, 10, 1, 9, 10));
        expedition.registerIncident(diveItem, IncidentReport.of("Current stronger than forecast", LocalDateTime.of(2026, 10, 1, 10, 0)));
        expedition.finishActivity(diveItem, LocalDateTime.of(2026, 10, 1, 12, 0), "Reef mapped");
        expedition.startActivity(surveyItem, LocalDateTime.of(2026, 10, 1, 13, 0));
        expedition.finishActivity(surveyItem, LocalDateTime.of(2026, 10, 1, 15, 0), "Six samples collected");
        finishExpedition.finish(expedition);

        final var report = reporter.reportOf(expedition);
        assertEquals(ExpeditionStatus.FINISHED, report.status());
        assertTrue(report.isComplete());
        assertEquals(RiskLevel.HIGH, report.estimate().overallRisk());
        assertEquals(Map.of(FUEL, Quantity.of("20", LITERS), SAMPLE_CONTAINER, Quantity.of("3", UNITS)),
                report.actualConsumption());
        assertEquals(1, report.incidents().size());
        assertEquals(List.of(acceptance), report.acceptedWarnings());
        assertEquals(Quantity.of("80", LITERS), fuel.getStock());
        assertEquals(Quantity.of("17", UNITS), jars.getStock());
        assertTrue(leader.isAvailableDuring(divePeriod));
    }

    @Test
    void replanningInTheFieldEndToEnd() {
        final var original = approvedReefSurvey();
        original.start();
        final var originalDive = itemOf(original, dive);
        original.startActivity(originalDive, LocalDateTime.of(2026, 10, 1, 9, 0));
        original.finishActivity(originalDive, LocalDateTime.of(2026, 10, 1, 12, 0), "Reef mapped");
        original.suspend();

        final var alternative = replanExpedition
                .replan(original, itemOf(original, survey), new Delay(Duration.ofHours(1)));
        alternative.submitForReview();
        approveExpedition.approve(alternative, List.of());

        assertEquals(ExpeditionStatus.REPLACED, original.getStatus());
        assertEquals(Optional.of(alternative.getId()), original.getReplacement());
        assertEquals(ExpeditionStatus.APPROVED, alternative.getStatus());
        assertTrue(itemOf(alternative, dive).isFinished());
        final var delayedSurvey = itemOf(alternative, survey);
        assertEquals(hours(14, 16), delayedSurvey.getScheduledPeriod());

        alternative.start();
        alternative.startActivity(delayedSurvey, LocalDateTime.of(2026, 10, 1, 14, 0));
        alternative.finishActivity(delayedSurvey, LocalDateTime.of(2026, 10, 1, 16, 0), "Six samples collected");
        finishExpedition.finish(alternative);

        final var report = reporter.reportOf(alternative);
        assertEquals(ExpeditionStatus.FINISHED, report.status());
        assertTrue(report.isComplete());
        assertEquals(Map.of(FUEL, Quantity.of("20", LITERS), SAMPLE_CONTAINER, Quantity.of("3", UNITS)),
                report.actualConsumption());
        assertEquals(Quantity.of("80", LITERS), fuel.getStock());
        assertEquals(Quantity.of("17", UNITS), jars.getStock());
        assertTrue(jars.hasStockFor(Quantity.of("17", UNITS)));
    }

    @Test
    void anExpeditionMissingPermitsAndResourcesCannotBeApproved() {
        final var expedition = createExpedition.create(new ExpeditionBuilder()
                .withName("Unprepared dive")
                .withPeriod(hours(0, 24))
                .addObjective("Dive without a permit")
                .addZone(reef)
                .addResponsible(leader)
                .withRestrictions(new ExpeditionRestrictions(10, RiskLevel.HIGH)));
        expedition.schedule(dive, divePeriod);
        expedition.submitForReview();

        final var result = preview.validate(expedition);
        final var messages = result.criticals().stream().map(ValidationResult::message).toList();
        assertTrue(messages.stream().anyMatch(message -> message.contains("Marine reserve access")), messages::toString);
        assertTrue(messages.stream().anyMatch(message -> message.contains("staff")), messages::toString);
        assertFalse(result.canBeApproved());

        assertThrows(ExpeditionNotApprovableException.class, () -> approveExpedition.approve(expedition, List.of()));
        assertEquals(ExpeditionStatus.IN_REVIEW, expedition.getStatus());
        assertTrue(leader.isAvailableDuring(divePeriod));
    }
}
