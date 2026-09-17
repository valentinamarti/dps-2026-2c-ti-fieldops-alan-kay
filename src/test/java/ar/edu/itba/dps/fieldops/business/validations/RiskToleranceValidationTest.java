package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.diving;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskToleranceValidationTest {

    private final RiskToleranceValidation validation = new RiskToleranceValidation();
    private final Zone reef = zone("z-1");
    private final Expedition expedition = expeditionIn(reef, person("p-1")).build();

    @Test
    void warnsAboutAnActivityRiskierThanTheExpeditionTolerance() {
        expedition.schedule(diving("a-1", reef, hours(8, 18), 40), hours(9, 12));

        final var expected = ValidationResult.warning("Diving a-1 has HIGH risk, above the expedition tolerance of MEDIUM");
        assertEquals(List.of(expected), validation.validate(expedition));
    }

    @Test
    void acceptsActivitiesWhoseRiskIsWithinTheTolerance() {
        expedition.schedule(diving("a-1", reef, hours(8, 18), 20), hours(9, 12));

        assertTrue(validation.validate(expedition).isEmpty());
    }
}
