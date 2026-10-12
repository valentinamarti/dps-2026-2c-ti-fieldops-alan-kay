package ar.edu.itba.dps.fieldops.business.models.activities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskLevelTest {

    @ParameterizedTest(name = "after {0} comes {1}")
    @CsvSource({"LOW, MEDIUM", "MEDIUM, HIGH", "HIGH, EXTREME", "EXTREME, EXTREME"})
    void theNextLevelIsOneStepUpAndTheMaximumHasNoneAbove(RiskLevel level, RiskLevel next) {
        assertEquals(next, level.next());
    }

    @Test
    void extremeIsAboveHigh() {
        assertTrue(RiskLevel.EXTREME.isAbove(RiskLevel.HIGH));
        assertFalse(RiskLevel.HIGH.isAbove(RiskLevel.EXTREME));
    }
}
