package ar.edu.itba.dps.fieldops.business.integration;

import ar.edu.itba.dps.fieldops.business.estimations.ExpeditionEstimator;
import ar.edu.itba.dps.fieldops.business.exceptions.ExpeditionNotApprovableException;
import ar.edu.itba.dps.fieldops.business.interfaces.validation.ExpeditionValidator;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionBuilder;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionRestrictions;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Incident;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.resources.Depletable;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.validation.AcceptedWarning;
import ar.edu.itba.dps.fieldops.business.models.validation.Severity;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.reports.ExpeditionReporter;
import ar.edu.itba.dps.fieldops.business.validations.CapacityExceededValidation;
import ar.edu.itba.dps.fieldops.business.validations.InsufficientSuppliesValidation;
import ar.edu.itba.dps.fieldops.business.validations.MissingCertificationsValidation;
import ar.edu.itba.dps.fieldops.business.validations.MissingPermitsValidation;
import ar.edu.itba.dps.fieldops.business.validations.MissingResourcesValidation;
import ar.edu.itba.dps.fieldops.business.validations.ResourceOverlapValidation;
import ar.edu.itba.dps.fieldops.business.validations.RiskToleranceValidation;
import ar.edu.itba.dps.fieldops.business.validations.ValidationOrchestrator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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


class ExpeditionLifecycleIntegrationTest {

    private static final Permit MARINE_RESERVE = new Permit("Marine reserve access");
    private static final ResourceCategory FUEL = new ResourceCategory("Fuel");
    private static final ResourceCategory SAMPLE_CONTAINER = new ResourceCategory("Sample container");

    private final Zone reef = new Zone("z-1", "Protected reef", java.util.Set.of(MARINE_RESERVE));
    private final Person leader = person("p-1", DIVING);
    private final Person secondDiver = person("p-2", DIVING);
    private final Person fieldWorker = person("p-3");
    private final Depletable fuel = supply("d-1", "Fuel", Quantity.of("100", LITERS));
    private final Depletable jars = supply("d-2", "Sample container", Quantity.of("20", UNITS));

    private final ResourceCatalog catalog = new ResourceCatalog();
    private final ExpeditionValidator validator = new ValidationOrchestrator(List.of(
            new ResourceOverlapValidation(),
            new MissingResourcesValidation(),
            new InsufficientSuppliesValidation(),
            new MissingCertificationsValidation(),
            new MissingPermitsValidation(),
            new CapacityExceededValidation(),
            new RiskToleranceValidation()));
    private final ExpeditionReporter reporter = new ExpeditionReporter(new ExpeditionEstimator());

    private final TimePeriod divePeriod = hours(9, 12);
    private final TimePeriod samplingPeriod = hours(13, 15);
    private final Activity dive = diving("a-1", reef, hours(8, 18), 40);
    private final Activity survey = sampling("a-2", reef, hours(8, 18));

    ExpeditionLifecycleIntegrationTest() {
        catalog.addStaff(leader);
        catalog.addStaff(secondDiver);
        catalog.addStaff(fieldWorker);
        catalog.addEquipment(vehicle("v-1", "Boat"));
        catalog.addEquipment(instrument("i-1", "Oxygen tank"));
        catalog.addEquipment(instrument("i-2", "Oxygen tank"));
        catalog.addEquipment(instrument("i-3", "Cooler"));
        catalog.addSupply(fuel);
        catalog.addSupply(jars);
    }

    private Expedition draft() {
        return new ExpeditionBuilder()
                .withId("e-1")
                .withName("Reef survey")
                .withPeriod(hours(0, 24))
                .addObjective("Map the reef and collect water samples")
                .addZone(reef)
                .addResponsible(leader)
                .addPermit(MARINE_RESERVE)
                .withRestrictions(new ExpeditionRestrictions(10, RiskLevel.MEDIUM))
                .build();
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

    @Test
    void planningApprovingAndExecutingAnExpeditionEndToEnd() {
        final var expedition = draft();
        final var diveItem = expedition.schedule(dive, divePeriod);
        final var surveyItem = expedition.schedule(survey, samplingPeriod);
        assignFromCatalog(expedition, diveItem);
        assignFromCatalog(expedition, surveyItem);

        final var result = validator.validate(expedition);
        assertTrue(result.criticals().isEmpty(), () -> "unexpected criticals: " + result.criticals());
        assertEquals(1, result.warnings().size());
        assertEquals(Severity.WARNING, result.warnings().getFirst().severity());

        expedition.submitForReview();
        final var acceptance = new AcceptedWarning(result.warnings().getFirst(), leader, "Both divers are instructors");
        expedition.approve(validator, List.of(acceptance));
        assertEquals(ExpeditionStatus.APPROVED, expedition.getStatus());
        assertFalse(leader.isAvailableDuring(divePeriod));

        expedition.start();
        expedition.startActivity(diveItem, LocalDateTime.of(2026, 10, 1, 9, 10));
        expedition.reportIncident(diveItem, new Incident("Current stronger than forecast", LocalDateTime.of(2026, 10, 1, 10, 0)));
        expedition.finishActivity(diveItem, LocalDateTime.of(2026, 10, 1, 12, 0), "Reef mapped");
        expedition.startActivity(surveyItem, LocalDateTime.of(2026, 10, 1, 13, 0));
        expedition.finishActivity(surveyItem, LocalDateTime.of(2026, 10, 1, 15, 0), "Six samples collected");
        expedition.finish();

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
    }

    @Test
    void anExpeditionMissingPermitsAndResourcesCannotBeApproved() {
        final var expedition = new ExpeditionBuilder()
                .withId("e-2")
                .withName("Unprepared dive")
                .withPeriod(hours(0, 24))
                .addObjective("Dive without a permit")
                .addZone(reef)
                .addResponsible(leader)
                .withRestrictions(new ExpeditionRestrictions(10, RiskLevel.HIGH))
                .build();
        expedition.schedule(dive, divePeriod);
        expedition.submitForReview();

        final var result = validator.validate(expedition);
        final var messages = result.criticals().stream().map(critical -> critical.message()).toList();
        assertTrue(messages.stream().anyMatch(message -> message.contains("Marine reserve access")), () -> messages.toString());
        assertTrue(messages.stream().anyMatch(message -> message.contains("staff")), () -> messages.toString());
        assertFalse(result.canBeApproved());

        assertThrows(ExpeditionNotApprovableException.class, () -> expedition.approve(validator, List.of()));
        assertEquals(ExpeditionStatus.IN_REVIEW, expedition.getStatus());
        assertTrue(leader.isAvailableDuring(divePeriod));
    }
}
