package ar.edu.itba.dps.fieldops.business.integration;

import ar.edu.itba.dps.fieldops.business.exceptions.ExpeditionNotApprovableException;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.commands.RegisterIncidentCommand;
import ar.edu.itba.dps.fieldops.business.models.commands.WarningAcceptance;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionBuilder;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionRestrictions;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.expeditions.IncidentReport;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.resources.Depletable;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.ports.in.ApproveExpeditionPort;
import ar.edu.itba.dps.fieldops.business.ports.in.FinishExpeditionPort;
import ar.edu.itba.dps.fieldops.business.ports.in.RegisterIncidentPort;
import ar.edu.itba.dps.fieldops.business.ports.in.TrackActivityPort;
import ar.edu.itba.dps.fieldops.business.ports.in.TransitionExpeditionPort;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.validation.AcceptedWarning;
import ar.edu.itba.dps.fieldops.business.models.validation.Severity;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;
import ar.edu.itba.dps.fieldops.business.replanning.Delay;
import ar.edu.itba.dps.fieldops.business.reports.ExpeditionReporter;
import ar.edu.itba.dps.fieldops.business.usecases.ReplanExpedition;
import ar.edu.itba.dps.fieldops.business.validations.ApprovalPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

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
@Sql("/clean-database.sql")
class ExpeditionLifecycleIntegrationTest {

    private static final Permit MARINE_RESERVE = new Permit("Marine reserve access");
    private static final ResourceCategory FUEL = new ResourceCategory("Fuel");
    private static final ResourceCategory SAMPLE_CONTAINER = new ResourceCategory("Sample container");
    private static final String JUSTIFICATION = "Both divers are instructors";

    @Autowired
    private ExpeditionRepository expeditions;
    @Autowired
    private ZoneRepository zones;
    @Autowired
    private ResourceCatalog catalog;
    @Autowired
    private ApproveExpeditionPort approveExpedition;
    @Autowired
    private TransitionExpeditionPort transitions;
    @Autowired
    private TrackActivityPort tracking;
    @Autowired
    private RegisterIncidentPort registerIncident;
    @Autowired
    private ReplanExpedition replanExpedition;
    @Autowired
    private FinishExpeditionPort finishExpedition;
    @Autowired
    private ExpeditionReporter reporter;

    private final ApprovalPolicy preview = new ApprovalPolicy(List.of());
    private final Zone reef = new Zone("z-1", "Protected reef", Set.of(MARINE_RESERVE));
    private final Person leader = person("p-1", DIVING);
    private final Depletable fuel = supply("d-1", "Fuel", Quantity.of("100", LITERS));
    private final Depletable jars = supply("d-2", "Sample container", Quantity.of("20", UNITS));
    private final TimePeriod divePeriod = hours(9, 12);
    private final TimePeriod samplingPeriod = hours(13, 15);
    private final Activity dive = diving("a-1", reef, hours(8, 18), 40);
    private final Activity survey = sampling("a-2", reef, hours(8, 18));

    private static LocalDateTime at(int hour) {
        return LocalDateTime.of(2026, 10, 1, hour, 0);
    }

    @BeforeEach
    void registerTheCatalog() {
        zones.add(reef);
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

    private Expedition plannedReefSurvey() {
        final var expedition = reefSurvey().build(expeditions);
        assignFromCatalog(expedition, expedition.schedule(dive, divePeriod));
        assignFromCatalog(expedition, expedition.schedule(survey, samplingPeriod));
        return expedition;
    }

    private WarningAcceptance acceptanceOf(ValidationResult warning) {
        return new WarningAcceptance(warning.rule(), warning.subject(), leader.getId(), JUSTIFICATION);
    }

    private ExpeditionId approvedReefSurvey() {
        final var expedition = plannedReefSurvey();
        final var riskyDive = preview.validate(expedition).warnings().getFirst();
        expedition.submitForReview();
        expeditions.add(expedition);
        approveExpedition.approve(expedition.getId(), List.of(acceptanceOf(riskyDive)));
        return expedition.getId();
    }

    private Expedition reloaded(ExpeditionId id) {
        return expeditions.findById(id).orElseThrow();
    }

    private void run(ExpeditionId id, Activity activity, int fromHour, int toHour, String result) {
        tracking.start(id, activity.getId(), at(fromHour));
        tracking.finish(id, activity.getId(), at(toHour), result);
    }

    private boolean leaderIsFreeDuringTheDive() {
        return catalog.findStaff(leader.getId()).orElseThrow().isAvailableDuring(divePeriod);
    }

    private void assertStocks(String fuelLeft, String jarsLeft) {
        final var currentFuel = (Depletable) catalog.findSupply(fuel.getId()).orElseThrow();
        final var currentJars = (Depletable) catalog.findSupply(jars.getId()).orElseThrow();
        assertEquals(Quantity.of(fuelLeft, LITERS), currentFuel.getStock());
        assertEquals(Quantity.of(jarsLeft, UNITS), currentJars.getStock());
    }

    @Test
    void planningApprovingAndExecutingAnExpeditionEndToEnd() {
        final var expedition = plannedReefSurvey();
        final var result = preview.validate(expedition);
        assertTrue(result.criticals().isEmpty(), () -> "unexpected criticals: " + result.criticals());
        assertEquals(1, result.warnings().size());
        assertEquals(Severity.WARNING, result.warnings().getFirst().severity());
        final var id = expedition.getId();
        expedition.submitForReview();
        expeditions.add(expedition);

        final var riskyDive = result.warnings().getFirst();
        approveExpedition.approve(id, List.of(acceptanceOf(riskyDive)));
        assertEquals(ExpeditionStatus.APPROVED, reloaded(id).getStatus());
        assertFalse(leaderIsFreeDuringTheDive());

        transitions.start(id);
        tracking.start(id, dive.getId(), at(9));
        registerIncident.register(new RegisterIncidentCommand(id, dive.getId(), IncidentReport.of("Current stronger than forecast", at(10))));
        tracking.finish(id, dive.getId(), at(12), "Reef mapped");
        run(id, survey, 13, 15, "Six samples collected");
        finishExpedition.finish(id);

        final var report = reporter.reportOf(reloaded(id));
        assertEquals(ExpeditionStatus.FINISHED, report.status());
        assertTrue(report.isComplete());
        assertEquals(RiskLevel.HIGH, report.estimate().overallRisk());
        assertEquals(Map.of(FUEL, Quantity.of("20", LITERS), SAMPLE_CONTAINER, Quantity.of("3", UNITS)),
                report.actualConsumption());
        assertEquals(1, report.incidents().size());
        assertEquals(List.of(new AcceptedWarning(riskyDive, leader, JUSTIFICATION)), report.acceptedWarnings());
        assertStocks("80", "17");
        assertTrue(leaderIsFreeDuringTheDive());
    }

    @Test
    void replanningInTheFieldEndToEnd() {
        final var originalId = approvedReefSurvey();
        transitions.start(originalId);
        run(originalId, dive, 9, 12, "Reef mapped");
        transitions.suspend(originalId);
        final var original = reloaded(originalId);

        final var alternativeId = replanExpedition
                .replan(original, original.getItinerary().itemFor(survey).orElseThrow(), new Delay(Duration.ofHours(1))).getId();
        transitions.submitForReview(alternativeId);
        approveExpedition.approve(alternativeId, List.of());

        assertEquals(ExpeditionStatus.REPLACED, reloaded(originalId).getStatus());
        assertEquals(Optional.of(alternativeId), reloaded(originalId).getReplacement());
        final var alternative = reloaded(alternativeId);
        assertEquals(ExpeditionStatus.APPROVED, alternative.getStatus());
        assertTrue(alternative.getItinerary().itemFor(dive).orElseThrow().isFinished());
        assertEquals(hours(14, 16), alternative.getItinerary().itemFor(survey).orElseThrow().getScheduledPeriod());

        transitions.start(alternativeId);
        run(alternativeId, survey, 14, 16, "Six samples collected");
        finishExpedition.finish(alternativeId);

        final var report = reporter.reportOf(reloaded(alternativeId));
        assertEquals(ExpeditionStatus.FINISHED, report.status());
        assertTrue(report.isComplete());
        assertEquals(Map.of(FUEL, Quantity.of("20", LITERS), SAMPLE_CONTAINER, Quantity.of("3", UNITS)),
                report.actualConsumption());
        assertStocks("80", "17");
    }

    @Test
    void anExpeditionMissingPermitsAndResourcesCannotBeApproved() {
        final var expedition = new ExpeditionBuilder()
                .withName("Unprepared dive")
                .withPeriod(hours(0, 24))
                .addObjective("Dive without a permit")
                .addZone(reef)
                .addResponsible(leader)
                .withRestrictions(new ExpeditionRestrictions(10, RiskLevel.HIGH))
                .build(expeditions);
        expedition.schedule(dive, divePeriod);
        expedition.submitForReview();
        expeditions.add(expedition);

        final var result = preview.validate(expedition);
        final var messages = result.criticals().stream().map(ValidationResult::message).toList();
        assertTrue(messages.stream().anyMatch(message -> message.contains("Marine reserve access")), messages::toString);
        assertTrue(messages.stream().anyMatch(message -> message.contains("staff")), messages::toString);
        assertFalse(result.canBeApproved());

        assertThrows(ExpeditionNotApprovableException.class, () -> approveExpedition.approve(expedition.getId(), List.of()));
        assertEquals(ExpeditionStatus.IN_REVIEW, reloaded(expedition.getId()).getStatus());
        assertTrue(leaderIsFreeDuringTheDive());
    }
}
