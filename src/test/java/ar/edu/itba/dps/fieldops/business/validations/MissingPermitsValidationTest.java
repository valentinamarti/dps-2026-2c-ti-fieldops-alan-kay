package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissingPermitsValidationTest {

    private static final Permit MARINE_RESERVE = new Permit("Marine reserve access");
    private static final Permit NATIONAL_PARK = new Permit("National park access");

    private final MissingPermitsValidation validation = new MissingPermitsValidation();
    private final Zone reserve = new Zone("z-1", "Marine reserve", Set.of(MARINE_RESERVE, NATIONAL_PARK));

    @Test
    void flagsEachPermitAZoneRequiresThatWasNotGranted() {
        final var expedition = expeditionIn(reserve, person("p-1")).addPermit(NATIONAL_PARK).build();

        final var expected = ValidationResult.critical("Marine reserve requires the permit Marine reserve access, which was not granted");
        assertEquals(List.of(expected), validation.validate(expedition));
    }

    @Test
    void acceptsAnExpeditionWithEveryPermitItsZonesRequire() {
        final var expedition = expeditionIn(reserve, person("p-1")).addPermit(NATIONAL_PARK).addPermit(MARINE_RESERVE).build();

        assertTrue(validation.validate(expedition).isEmpty());
    }
}