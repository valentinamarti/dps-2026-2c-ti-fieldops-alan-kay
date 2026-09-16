package ar.edu.itba.dps.fieldops.business.models.expeditions;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpeditionStatusTest {

    @ParameterizedTest
    @CsvSource({
            "DRAFT, IN_REVIEW",
            "IN_REVIEW, DRAFT",
            "IN_REVIEW, APPROVED",
            "APPROVED, IN_EXECUTION",
            "IN_EXECUTION, SUSPENDED",
            "IN_EXECUTION, FINISHED",
            "SUSPENDED, IN_EXECUTION",
            "SUSPENDED, FINISHED"
    })
    void followsTheApprovalAndExecutionFlow(ExpeditionStatus from, ExpeditionStatus to) {
        assertTrue(from.canTransitionTo(to));
    }

    @ParameterizedTest
    @CsvSource({
            "DRAFT, APPROVED",
            "DRAFT, IN_EXECUTION",
            "IN_REVIEW, IN_EXECUTION",
            "APPROVED, DRAFT",
            "IN_EXECUTION, DRAFT",
            "FINISHED, IN_EXECUTION"
    })
    void cannotSkipStepsOrGoBackOnceApproved(ExpeditionStatus from, ExpeditionStatus to) {
        assertFalse(from.canTransitionTo(to));
    }
}
