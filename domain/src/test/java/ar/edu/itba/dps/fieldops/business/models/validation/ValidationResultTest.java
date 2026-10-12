package ar.edu.itba.dps.fieldops.business.models.validation;

import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationResultTest {

    private static final RuleId RISK = new RuleId("risk-tolerance");
    private static final ValidationSubject FIRST_DIVE = ValidationSubject.of(new ActivityId("a-1"));

    @Test
    void twoResultsOfTheSameRuleAboutTheSameSubjectAreTheSameFindingWhateverTheirWording() {
        final var original = ValidationResult.warning(RISK, FIRST_DIVE, "Diving a-1 has HIGH risk");
        final var reworded = ValidationResult.warning(RISK, FIRST_DIVE, "Diving a-1 is riskier than allowed");

        assertTrue(original.isSameFindingAs(reworded));
    }

    @Test
    void resultsAboutDifferentSubjectsAreDifferentFindings() {
        final var aboutTheActivity = ValidationResult.warning(RISK, FIRST_DIVE, "Diving a-1 has HIGH risk");
        final var aboutAResourceInIt = ValidationResult.warning(RISK,
                ValidationSubject.of(new ActivityId("a-1"), new ResourceId("p-2")), "Diving a-1 has HIGH risk");

        assertFalse(aboutTheActivity.isSameFindingAs(aboutAResourceInIt));
    }

    @Test
    void resultsOfDifferentRulesAreDifferentFindings() {
        final var risk = ValidationResult.warning(RISK, FIRST_DIVE, "Diving a-1 has HIGH risk");
        final var other = ValidationResult.warning(new RuleId("another-rule"), FIRST_DIVE, "Diving a-1 has HIGH risk");

        assertFalse(risk.isSameFindingAs(other));
    }

    @Test
    void aResultAlwaysSaysWhichRuleProducedItAndWhatItIsAbout() {
        assertThrows(NullPointerException.class, () -> ValidationResult.critical(null, FIRST_DIVE, "Something is wrong"));
        assertThrows(NullPointerException.class, () -> ValidationResult.critical(RISK, null, "Something is wrong"));
    }
}
