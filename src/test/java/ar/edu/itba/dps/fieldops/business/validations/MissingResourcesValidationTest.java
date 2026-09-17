package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.instrument;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissingResourcesValidationTest {

    private final MissingResourcesValidation validation = new MissingResourcesValidation();
    private final Zone coast = zone("z-1");
    private final Expedition expedition = expeditionIn(coast, person("p-1")).build();
    private final ItineraryItem item = expedition.schedule(sampling("a-1", coast, hours(8, 18)), hours(9, 11));

    @Test
    void anActivityWithTheStaffAndEquipmentItNeedsHasNothingMissing() {
        expedition.assignStaff(item, person("p-2"));
        expedition.assignEquipment(item, instrument("i-1", "Cooler"));

        assertTrue(validation.validate(expedition).isEmpty());
    }

    @Test
    void flagsAnActivityWithoutEnoughStaff() {
        expedition.assignEquipment(item, instrument("i-1", "Cooler"));

        final var expected = ValidationResult.critical("Sampling a-1 has 0 of the 1 staff members it needs");
        assertEquals(List.of(expected), validation.validate(expedition));
    }

    @Test
    void equipmentOfAnotherCategoryDoesNotCoverTheRequirement() {
        expedition.assignStaff(item, person("p-2"));
        expedition.assignEquipment(item, instrument("i-1", "Sonar"));

        final var expected = ValidationResult.critical("Sampling a-1 has 0 of the 1 Cooler it needs");
        assertEquals(List.of(expected), validation.validate(expedition));
    }
}