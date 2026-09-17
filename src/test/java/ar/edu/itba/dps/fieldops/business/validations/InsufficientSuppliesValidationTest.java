package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.supply;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.LITERS;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsufficientSuppliesValidationTest {

    private final InsufficientSuppliesValidation validation = new InsufficientSuppliesValidation();
    private final Zone coast = zone("z-1");
    private final Expedition expedition = expeditionIn(coast, person("p-1")).build();

    private ItineraryItem scheduleSampling(String activityId, TimePeriod period) {
        return expedition.schedule(sampling(activityId, coast, hours(0, 24)), period);
    }

    @Test
    void acceptsASupplyWhoseStockCoversEveryActivityThatUsesIt() {
        final var jars = supply("d-1", "Sample container", Quantity.of("6", UNITS));
        expedition.assignSupply(scheduleSampling("a-1", hours(9, 11)), jars);
        expedition.assignSupply(scheduleSampling("a-2", hours(12, 14)), jars);

        assertTrue(validation.validate(expedition).isEmpty());
    }

    @Test
    void flagsAnActivityWithoutASupplyOfTheRequiredCategory() {
        expedition.assignSupply(scheduleSampling("a-1", hours(9, 11)), supply("d-1", "Fuel", Quantity.of("50", LITERS)));

        final var expected = ValidationResult.critical("Sampling a-1 needs 3 UNITS of Sample container but has no supply of that category assigned");
        assertEquals(List.of(expected), validation.validate(expedition));
    }

    @Test
    void flagsASupplyWhoseStockDoesNotCoverEveryActivityThatUsesIt() {
        final var jars = supply("d-1", "Sample container", Quantity.of("5", UNITS));
        expedition.assignSupply(scheduleSampling("a-1", hours(9, 11)), jars);
        expedition.assignSupply(scheduleSampling("a-2", hours(12, 14)), jars);

        final var expected = ValidationResult.critical("d-1 does not have enough stock for the 6 UNITS the itinerary needs");
        assertEquals(List.of(expected), validation.validate(expedition));
    }
}
