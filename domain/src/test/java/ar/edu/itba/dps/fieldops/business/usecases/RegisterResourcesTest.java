package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.DuplicateResourceException;
import ar.edu.itba.dps.fieldops.business.fixtures.FakeResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.commands.RegisterInstrumentCommand;
import ar.edu.itba.dps.fieldops.business.models.commands.RegisterPersonCommand;
import ar.edu.itba.dps.fieldops.business.models.commands.RegisterSupplyCommand;
import ar.edu.itba.dps.fieldops.business.models.commands.RegisterVehicleCommand;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.responses.PersonView;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.DIVING;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterResourcesTest {
    private final ResourceCatalog catalog = new FakeResourceCatalog();
    private final RegisterResources register = new RegisterResources(catalog);

    @Test
    void registersAPersonWithTheCertificationsItWasGiven() {
        final var ada = register.registerPerson(new RegisterPersonCommand(new ResourceId("p-1"), "Ada", Set.of(DIVING)));

        assertEquals(PersonView.of(catalog.findStaff(new ResourceId("p-1")).orElseThrow()), ada);
        assertEquals("Ada", ada.name());
        assertEquals(Set.of(DIVING), ada.certifications().values());
    }

    @Test
    void registersAVehicleWithItsLicensePlate() {
        final var zodiac = register.registerVehicle(new RegisterVehicleCommand(new ResourceId("v-1"), "Zodiac",
                new ResourceCategory("Boat"), "AA 123 BB", Set.of()));

        assertTrue(catalog.findEquipment(new ResourceId("v-1")).isPresent());
        assertEquals("AA 123 BB", zodiac.licensePlate());
        assertEquals(new ResourceCategory("boat"), zodiac.category());
    }

    @Test
    void registersAnInstrumentAsEquipmentToo() {
        final var cooler = register.registerInstrument(new RegisterInstrumentCommand(new ResourceId("i-1"), "Cooler",
                new ResourceCategory("Cooler"), Set.of()));

        assertEquals(List.of(cooler.id()), catalog.allEquipment().stream().map(equipment -> equipment.getId()).toList());
    }

    @Test
    void registersASupplyWithItsInitialStock() {
        final var jars = register.registerSupply(new RegisterSupplyCommand(new ResourceId("s-1"), "Sample containers",
                new ResourceCategory("Sample container"), Quantity.of("10", UNITS)));

        assertTrue(catalog.findSupply(new ResourceId("s-1")).isPresent());
        assertEquals(Quantity.of("10", UNITS), jars.stock());
    }

    @Test
    void normalizesTheCategoryAndTheCertificationsOfWhatItRegisters() {
        final var cooler = register.registerInstrument(new RegisterInstrumentCommand(new ResourceId("i-1"), "Cooler",
                new ResourceCategory(" Cooler "), Set.of(new Certification("Diving"))));

        assertEquals("cooler", cooler.category().name());
        assertEquals(Set.of(new Certification("diving")), cooler.certifications().values());
    }

    @Test
    void rejectsASecondResourceWithTheSameId() {
        register.registerPerson(new RegisterPersonCommand(new ResourceId("p-1"), "Ada", Set.of()));

        assertThrows(DuplicateResourceException.class,
                () -> register.registerPerson(new RegisterPersonCommand(new ResourceId("p-1"), "Grace", Set.of())));
    }

    @Test
    void rejectsAResourceOfAnotherKindWithAnIdThatIsAlreadyTaken() {
        register.registerPerson(new RegisterPersonCommand(new ResourceId("p-1"), "Ada", Set.of()));

        assertThrows(DuplicateResourceException.class, () -> register.registerVehicle(new RegisterVehicleCommand(
                new ResourceId("p-1"), "Zodiac", new ResourceCategory("Boat"), "AA 123 BB", Set.of())));
    }

    @Test
    void leavesAnIncompleteCommandToTheResourceItBuilds() {
        final var withoutName = new RegisterPersonCommand(new ResourceId("p-1"), null, Set.of());

        assertEquals("name is required",
                assertThrows(IllegalArgumentException.class, () -> register.registerPerson(withoutName)).getMessage());
        assertEquals(List.of(), catalog.allStaff());
    }
}
