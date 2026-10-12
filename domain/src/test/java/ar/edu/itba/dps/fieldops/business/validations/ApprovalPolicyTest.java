package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.interfaces.validation.ValidationRule;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.validation.RuleId;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationSubject;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.NIGHT_OPERATIONS;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.assignContainers;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.assignCooler;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.assignLighting;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.completeAssignments;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.diving;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.nightPermit;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.nightSampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.activity;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.parallel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApprovalPolicyTest {

    private static final ValidationResult MISSING_PERMIT = ValidationResult.critical(new RuleId("missing-permits"), ValidationSubject.expedition(), "Zone z-1 requires a permit");
    private static final ValidationResult RISKY_DIVE = ValidationResult.warning(new RuleId("risk-tolerance"), ValidationSubject.of(new ActivityId("a-1")), "Diving a-1 has HIGH risk");

    private final SequentialExpeditionRepository repository = new SequentialExpeditionRepository();
    private final Zone coast = zone("z-1");
    private final Expedition expedition = expeditionIn(coast, person("p-1")).build(repository);

    private static ApprovalPolicy policyAdding(ValidationRule... rules) {
        return new ApprovalPolicy(List.of(rules));
    }

    @Test
    void theMandatoryRulesRunEvenWithoutAdditionalRules() {
        expedition.schedule(diving("a-1", coast, hours(0, 24), 30), hours(9, 11));

        final var result = policyAdding().validate(expedition);

        assertFalse(result.canBeApproved());
        assertFalse(result.criticals().isEmpty());
    }

    @Test
    void anExpeditionWithoutProblemsPassesTheMandatoryRules() {
        assertTrue(policyAdding().validate(expedition).results().isEmpty());
    }

    @Test
    void combinesTheResultsOfTheAdditionalRulesSeparatingCriticalsFromWarnings() {
        final var policy = policyAdding(
                _ -> List.of(MISSING_PERMIT),
                _ -> List.of(RISKY_DIVE),
                _ -> List.of());

        final var result = policy.validate(expedition);

        assertEquals(List.of(MISSING_PERMIT), result.criticals());
        assertEquals(List.of(RISKY_DIVE), result.warnings());
    }

    @Test
    void anAdditionalCriticalProblemPreventsTheApproval() {
        assertFalse(policyAdding(_ -> List.of(RISKY_DIVE, MISSING_PERMIT)).validate(expedition).canBeApproved());
    }

    @Test
    void anExpeditionWithOnlyWarningsCanBeApproved() {
        assertTrue(policyAdding(_ -> List.of(RISKY_DIVE)).validate(expedition).canBeApproved());
    }

    @Test
    void aResourceConflictInAParallelBlockIsReportedOnceAndPreventsTheApproval() {
        final var first = sampling("a-1", coast, hours(0, 24));
        final var second = sampling("a-2", coast, hours(0, 24));
        final var worker = person("p-2");
        expedition.schedule(parallel(activity(first, hours(9, 11)), activity(second, hours(9, 11))));
        expedition.getItinerary().getItems().forEach(item -> expedition.assignStaff(item, worker));
        completeAssignments(expedition);

        final var result = policyAdding().validate(expedition);

        final var expected = ValidationResult.critical(ResourceOverlapValidation.ID, ValidationSubject.of(new ActivityId("a-1"), new ResourceId("p-2")), "p-2 is assigned to Sampling a-1 and Sampling a-2 at the same time");
        assertEquals(List.of(expected), result.results());
        assertFalse(result.canBeApproved());
    }

    @Test
    void aNightActivityWithoutItsCertificationAndItsPermitPreventsTheApproval() {
        final var night = expedition.schedule(nightSampling("a-1", coast, hours(0, 24)), hours(20, 22));
        expedition.assignStaff(night, person("p-2"));
        assignCooler(expedition, night);
        assignLighting(expedition, night);
        assignContainers(expedition, night);

        final var result = policyAdding().validate(expedition);

        final var certification = ValidationResult.critical(MissingCertificationsValidation.ID, ValidationSubject.of(new ActivityId("a-1"), new ResourceId("p-2")), "Night sampling a-1 requires everyone assigned to be certified in night operations, but p-2 is not");
        final var permit = ValidationResult.critical(MissingPermitsValidation.ID, ValidationSubject.of(new ActivityId("a-1")), "Night sampling a-1 requires the permit night operations in zone z-1, which was not granted");
        assertEquals(List.of(certification, permit), result.results());
        assertFalse(result.canBeApproved());
    }

    @Test
    void aNightActivityWithItsCertificationItsPermitAndLightingPassesTheMandatoryRules() {
        final var permitted = expeditionIn(coast, person("p-1")).addPermit(nightPermit(coast)).build(repository);
        final var night = permitted.schedule(nightSampling("a-1", coast, hours(0, 24)), hours(20, 22));
        permitted.assignStaff(night, person("p-2", NIGHT_OPERATIONS));
        assignCooler(permitted, night);
        assignLighting(permitted, night);
        assignContainers(permitted, night);

        final var result = policyAdding().validate(permitted);

        assertTrue(result.results().isEmpty());
        assertTrue(result.canBeApproved());
    }
}
