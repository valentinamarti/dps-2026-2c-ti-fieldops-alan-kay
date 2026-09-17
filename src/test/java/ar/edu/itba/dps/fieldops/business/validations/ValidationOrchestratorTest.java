package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.interfaces.validation.ValidationRule;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationOrchestratorTest {

    private static final ValidationResult MISSING_PERMIT = ValidationResult.critical("Zone z-1 requires a permit");
    private static final ValidationResult RISKY_DIVE = ValidationResult.warning("Diving a-1 has HIGH risk");

    private final Expedition expedition = expeditionIn(zone("z-1"), person("p-1")).build();

    private static ValidationOrchestrator orchestratorWith(ValidationRule... rules) {
        return new ValidationOrchestrator(List.of(rules));
    }

    @Test
    void combinesTheResultsOfEveryRuleSeparatingCriticalsFromWarnings() {
        final var orchestrator = orchestratorWith(
                expedition -> List.of(MISSING_PERMIT),
                expedition -> List.of(RISKY_DIVE),
                expedition -> List.of());

        final var result = orchestrator.validate(expedition);

        assertEquals(List.of(MISSING_PERMIT), result.criticals());
        assertEquals(List.of(RISKY_DIVE), result.warnings());
    }

    @Test
    void anExpeditionWithACriticalProblemCannotBeApproved() {
        final var orchestrator = orchestratorWith(expedition -> List.of(RISKY_DIVE, MISSING_PERMIT));

        assertFalse(orchestrator.validate(expedition).canBeApproved());
    }

    @Test
    void anExpeditionWithOnlyWarningsCanBeApproved() {
        final var orchestrator = orchestratorWith(expedition -> List.of(RISKY_DIVE));

        assertTrue(orchestrator.validate(expedition).canBeApproved());
    }
}