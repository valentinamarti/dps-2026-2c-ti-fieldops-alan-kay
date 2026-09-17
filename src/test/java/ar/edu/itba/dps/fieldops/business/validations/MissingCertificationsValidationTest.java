package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.DIVING;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.diving;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissingCertificationsValidationTest {

    private final MissingCertificationsValidation validation = new MissingCertificationsValidation();
    private final Zone reef = zone("z-1");
    private final Expedition expedition = expeditionIn(reef, person("p-1")).build();
    private final ItineraryItem dive = expedition.schedule(diving("a-1", reef, hours(8, 18), 10), hours(9, 11));

    @Test
    void acceptsADiveWithTwoCertifiedDivers() {
        expedition.assignStaff(dive, person("p-2", DIVING));
        expedition.assignStaff(dive, person("p-3", DIVING));

        assertTrue(validation.validate(expedition).isEmpty());
    }

    @Test
    void flagsADiveWithoutEnoughCertifiedDivers() {
        expedition.assignStaff(dive, person("p-2", DIVING));
        expedition.assignStaff(dive, person("p-3"));

        final var expected = ValidationResult.critical("Diving a-1 needs 2 people certified in Diving but only 1 qualify");
        assertEquals(List.of(expected), validation.validate(expedition));
    }
}
