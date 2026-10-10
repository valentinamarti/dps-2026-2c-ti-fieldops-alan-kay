package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.interfaces.validation.ValidationRule;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.validation.RuleId;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationSubject;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.EXPEDITION_ID;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.diving;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApprovalPolicyTest {

    private static final ValidationResult MISSING_PERMIT = ValidationResult.critical(new RuleId("missing-permits"), ValidationSubject.expedition(), "Zone z-1 requires a permit");
    private static final ValidationResult RISKY_DIVE = ValidationResult.warning(new RuleId("risk-tolerance"), ValidationSubject.of(new ActivityId("a-1")), "Diving a-1 has HIGH risk");

    private final Zone coast = zone("z-1");
    private final Expedition expedition = expeditionIn(coast, person("p-1")).build(EXPEDITION_ID);

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
}
