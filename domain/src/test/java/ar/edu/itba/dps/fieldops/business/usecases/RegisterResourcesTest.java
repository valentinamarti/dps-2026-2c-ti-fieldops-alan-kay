package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.DuplicateResourceException;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.DIVING;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The catalog is a model object without I/O, so these tests use the real one instead of a double.
 */
class RegisterResourcesTest {

    private final ResourceCatalog catalog = new ResourceCatalog();
    private final RegisterResources register = new RegisterResources(catalog);

    @Test
    void registersAPersonWithTheCertificationsItWasGiven() {
        final var ada = register.registerPerson(new RegisterPersonCommand(new ResourceId("p-1"), "Ada", Set.of(DIVING)));

        assertSame(ada, catalog.findStaff(new ResourceId("p-1")).orElseThrow());
        assertEquals("Ada", ada.getName());
        assertEquals(Set.of(DIVING), ada.getCertifications().values());
    }

    @Test
    void registersAVehicleWithItsLicensePlate() {
        final var zodiac = register.registerVehicle(new RegisterVehicleCommand(new ResourceId("v-1"), "Zodiac",
                new ResourceCategory("Boat"), "AA 123 BB", Set.of()));

        assertSame(zodiac, catalog.findEquipment(new ResourceId("v-1")).orElseThrow());
        assertEquals("AA 123 BB", zodiac.getLicensePlate());
        assertEquals(new ResourceCategory("boat"), zodiac.getCategory());
    }

    @Test
    void registersAnInstrumentAsEquipmentToo() {
        final var cooler = register.registerInstrument(new RegisterInstrumentCommand(new ResourceId("i-1"), "Cooler",
                new ResourceCategory("Cooler"), Set.of()));

        assertEquals(List.of(cooler), catalog.allEquipment());
    }

    @Test
    void registersASupplyWithItsInitialStock() {
        final var jars = register.registerSupply(new RegisterSupplyCommand(new ResourceId("s-1"), "Sample containers",
                new ResourceCategory("Sample container"), Quantity.of("10", UNITS)));

        assertSame(jars, catalog.findSupply(new ResourceId("s-1")).orElseThrow());
        assertEquals(Quantity.of("10", UNITS), jars.getStock());
    }

    /**
     * The normalization of D34 happens when the value object is built, so what comes back is the normalized
     * name and not the one that was sent.
     */
    @Test
    void normalizesTheCategoryAndTheCertificationsOfWhatItRegisters() {
        final var cooler = register.registerInstrument(new RegisterInstrumentCommand(new ResourceId("i-1"), "Cooler",
                new ResourceCategory(" Cooler "), Set.of(new Certification("Diving"))));

        assertEquals("cooler", cooler.getCategory().name());
        assertEquals(Set.of(new Certification("diving")), cooler.getCertifications().values());
    }

    @Test
    void rejectsASecondResourceWithTheSameId() {
        register.registerPerson(new RegisterPersonCommand(new ResourceId("p-1"), "Ada", Set.of()));

        assertThrows(DuplicateResourceException.class,
                () -> register.registerPerson(new RegisterPersonCommand(new ResourceId("p-1"), "Grace", Set.of())));
    }

    /**
     * The catalog keeps a single id space, so the id of a person is taken for a vehicle as well.
     */
    @Test
    void rejectsAResourceOfAnotherKindWithAnIdThatIsAlreadyTaken() {
        register.registerPerson(new RegisterPersonCommand(new ResourceId("p-1"), "Ada", Set.of()));

        assertThrows(DuplicateResourceException.class, () -> register.registerVehicle(new RegisterVehicleCommand(
                new ResourceId("p-1"), "Zodiac", new ResourceCategory("Boat"), "AA 123 BB", Set.of())));
    }

    /**
     * The use case does not validate: what rejects an incomplete command is the constructor of the resource,
     * which is the only place where the rules of a resource live (D16).
     */
    @Test
    void leavesAnIncompleteCommandToTheResourceItBuilds() {
        final var withoutName = new RegisterPersonCommand(new ResourceId("p-1"), null, Set.of());

        assertEquals("name is required",
                assertThrows(IllegalArgumentException.class, () -> register.registerPerson(withoutName)).getMessage());
        assertEquals(List.of(), catalog.allStaff());
    }
}
