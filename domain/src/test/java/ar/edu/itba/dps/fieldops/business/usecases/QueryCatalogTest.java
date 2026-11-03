package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.NotFoundException;
import ar.edu.itba.dps.fieldops.business.fixtures.FakeResourceCatalog;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.responses.InstrumentView;
import ar.edu.itba.dps.fieldops.business.models.responses.PersonView;
import ar.edu.itba.dps.fieldops.business.models.responses.SupplyView;
import ar.edu.itba.dps.fieldops.business.models.responses.VehicleView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.instrument;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.supply;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.vehicle;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;
import static ar.edu.itba.dps.fieldops.business.models.common.Quantity.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryCatalogTest {
    private final ResourceCatalog catalog = new FakeResourceCatalog();
    private final QueryCatalog query = new QueryCatalog(catalog);

    @Test
    void listsNothingWhenNothingWasRegistered() {
        assertTrue(query.staff().isEmpty());
        assertTrue(query.vehicles().isEmpty());
        assertTrue(query.instruments().isEmpty());
        assertTrue(query.supplies().isEmpty());
    }

    @Test
    void listsEveryKindInTheOrderItWasRegistered() {
        final var ada = person("p-1");
        final var grace = person("p-2");
        catalog.addStaff(ada);
        catalog.addStaff(grace);

        assertEquals(List.of(PersonView.of(ada), PersonView.of(grace)), query.staff());
    }

    @Test
    void keepsVehiclesAndInstrumentsApartEvenThoughBothAreEquipment() {
        final var zodiac = vehicle("v-1", "Boat");
        final var cooler = instrument("i-1", "Cooler");
        catalog.addEquipment(zodiac);
        catalog.addEquipment(cooler);

        assertEquals(List.of(VehicleView.of(zodiac)), query.vehicles());
        assertEquals(List.of(InstrumentView.of(cooler)), query.instruments());
    }

    @Test
    void findsEachKindByItsId() {
        final var ada = person("p-1");
        final var zodiac = vehicle("v-1", "Boat");
        final var cooler = instrument("i-1", "Cooler");
        final var jars = supply("s-1", "Sample container", of("10", UNITS));
        catalog.addStaff(ada);
        catalog.addEquipment(zodiac);
        catalog.addEquipment(cooler);
        catalog.addSupply(jars);

        assertEquals(PersonView.of(ada), query.person(new ResourceId("p-1")));
        assertEquals(VehicleView.of(zodiac), query.vehicle(new ResourceId("v-1")));
        assertEquals(InstrumentView.of(cooler), query.instrument(new ResourceId("i-1")));
        assertEquals(SupplyView.of(jars), query.supply(new ResourceId("s-1")));
    }

    @Test
    void failsWithTheKindThatWasAskedForWhenNothingIsRegisteredUnderThatId() {
        final var unknown = new ResourceId("x-404");

        assertEquals("There is no person with id x-404",
                assertThrows(NotFoundException.class, () -> query.person(unknown)).getMessage());
        assertEquals("There is no vehicle with id x-404",
                assertThrows(NotFoundException.class, () -> query.vehicle(unknown)).getMessage());
        assertEquals("There is no instrument with id x-404",
                assertThrows(NotFoundException.class, () -> query.instrument(unknown)).getMessage());
        assertEquals("There is no supply with id x-404",
                assertThrows(NotFoundException.class, () -> query.supply(unknown)).getMessage());
    }

    @Test
    void doesNotFindAVehicleWhenTheIdBelongsToAnInstrument() {
        catalog.addEquipment(instrument("i-1", "Cooler"));

        assertEquals("There is no vehicle with id i-1",
                assertThrows(NotFoundException.class, () -> query.vehicle(new ResourceId("i-1"))).getMessage());
    }
}
