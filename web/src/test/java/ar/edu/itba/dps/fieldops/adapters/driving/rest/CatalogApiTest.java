package ar.edu.itba.dps.fieldops.adapters.driving.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CatalogApiTest {
    private static final String DIVER = """
            {"id": "p-1", "name": "Ada", "certifications": ["Diving"]}
            """;

    private static final String ZODIAC = """
            {"id": "v-1", "name": "Zodiac", "category": "Boat", "licensePlate": "AA 123 BB", "certifications": ["Boat licence"]}
            """;

    private static final String COOLER = """
            {"id": "i-1", "name": "Field cooler", "category": "Cooler", "certifications": []}
            """;

    private static final String JARS = """
            {"id": "s-1", "name": "Sample containers", "category": "Sample container",
             "initialStock": {"amount": "10", "unit": "UNITS"}}
            """;

    @Autowired
    private MockMvc api;

    @Test
    void registersAPersonAndSaysWhereToFindIt() throws Exception {
        register("/api/staff", DIVER)
                .andExpect(header().string("Location", "/api/staff/p-1"))
                .andExpect(jsonPath("$.id").value("p-1"))
                .andExpect(jsonPath("$.name").value("Ada"));

        api.perform(get("/api/staff/{id}", "p-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ada"));
    }

    @Test
    void normalizesTheCertificationsOfThePersonItRegisters() throws Exception {
        register("/api/staff", DIVER).andExpect(jsonPath("$.certifications[0]").value("diving"));
    }

    @Test
    void registersAVehicleWithItsLicensePlate() throws Exception {
        register("/api/vehicles", ZODIAC)
                .andExpect(header().string("Location", "/api/vehicles/v-1"))
                .andExpect(jsonPath("$.category").value("boat"))
                .andExpect(jsonPath("$.licensePlate").value("AA 123 BB"));

        api.perform(get("/api/vehicles/{id}", "v-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licensePlate").value("AA 123 BB"));
    }

    @Test
    void registersAnInstrumentWithoutALicensePlate() throws Exception {
        register("/api/instruments", COOLER)
                .andExpect(header().string("Location", "/api/instruments/i-1"))
                .andExpect(jsonPath("$.category").value("cooler"))
                .andExpect(jsonPath("$.licensePlate").doesNotExist());

        api.perform(get("/api/instruments/{id}", "i-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Field cooler"));
    }

    @Test
    void registersASupplyWithItsStock() throws Exception {
        register("/api/supplies", JARS)
                .andExpect(header().string("Location", "/api/supplies/s-1"))
                .andExpect(jsonPath("$.stock.amount").value("10"))
                .andExpect(jsonPath("$.stock.unit").value("UNITS"));

        api.perform(get("/api/supplies/{id}", "s-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock.amount").value("10"));
    }

    @Test
    void listsEachKindOfResourceOnItsOwnResource() throws Exception {
        register("/api/staff", DIVER);
        register("/api/vehicles", ZODIAC);
        register("/api/instruments", COOLER);
        register("/api/supplies", JARS);

        api.perform(get("/api/staff")).andExpect(jsonPath("$.length()").value(1));
        api.perform(get("/api/vehicles"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("v-1"));
        api.perform(get("/api/instruments"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("i-1"));
        api.perform(get("/api/supplies")).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listsTheVehiclesAndTheInstrumentsTogetherUnderEquipment() throws Exception {
        register("/api/vehicles", ZODIAC);
        register("/api/instruments", COOLER);

        api.perform(get("/api/equipment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicles.length()").value(1))
                .andExpect(jsonPath("$.vehicles[0].licensePlate").value("AA 123 BB"))
                .andExpect(jsonPath("$.instruments.length()").value(1))
                .andExpect(jsonPath("$.instruments[0].id").value("i-1"));
    }

    @Test
    void doesNotFindAnInstrumentUnderTheVehiclesResource() throws Exception {
        register("/api/instruments", COOLER);

        api.perform(get("/api/vehicles/{id}", "i-1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no vehicle with id i-1"));
    }

    @Test
    void rejectsASecondResourceWithAnIdThatIsAlreadyTaken() throws Exception {
        register("/api/staff", DIVER);

        api.perform(post("/api/staff").contentType(MediaType.APPLICATION_JSON).content(DIVER))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("A resource with id p-1 is already registered"));
    }

    @Test
    void rejectsAResourceOfAnotherKindWithAnIdThatIsAlreadyTaken() throws Exception {
        register("/api/staff", DIVER);

        api.perform(post("/api/vehicles").contentType(MediaType.APPLICATION_JSON)
                        .content(ZODIAC.replace("v-1", "p-1")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A resource with id p-1 is already registered"));
    }

    @Test
    void answersNotFoundForAResourceThatIsNotRegistered() throws Exception {
        api.perform(get("/api/staff/{id}", "p-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no person with id p-404"));
        api.perform(get("/api/supplies/{id}", "s-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no supply with id s-404"));
    }

    @Test
    void answersBadRequestWhenTheBodyDoesNotDescribeAResource() throws Exception {
        api.perform(post("/api/staff").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ada", "certifications": []}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("id is required"));
    }

    @Test
    void marksAReusableResourceUnavailableForAPeriod() throws Exception {
        register("/api/staff", DIVER);
        register("/api/vehicles", ZODIAC);
        register("/api/instruments", COOLER);

        api.perform(post("/api/staff/{id}/unavailabilities", "p-1").contentType(MediaType.APPLICATION_JSON).content("""
                        {"period": {"start": "2026-10-01T08:00:00", "end": "2026-10-01T12:00:00"}, "reason": "Sick leave"}
                        """))
                .andExpect(status().isNoContent());
        api.perform(post("/api/vehicles/{id}/unavailabilities", "v-1").contentType(MediaType.APPLICATION_JSON).content("""
                        {"period": {"start": "2026-10-01T08:00:00", "end": "2026-10-01T12:00:00"}, "reason": "Maintenance"}
                        """))
                .andExpect(status().isNoContent());
        api.perform(post("/api/instruments/{id}/unavailabilities", "i-1").contentType(MediaType.APPLICATION_JSON).content("""
                        {"period": {"start": "2026-10-01T08:00:00", "end": "2026-10-01T12:00:00"}, "reason": "Calibration"}
                        """))
                .andExpect(status().isNoContent());
    }

    @Test
    void answersNotFoundWhenTheResourceToMarkUnavailableIsNotRegistered() throws Exception {
        api.perform(post("/api/staff/{id}/unavailabilities", "p-404").contentType(MediaType.APPLICATION_JSON).content("""
                        {"period": {"start": "2026-10-01T08:00:00", "end": "2026-10-01T12:00:00"}, "reason": "Sick leave"}
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("There is no person with id p-404"));
    }

    @Test
    void doesNotOfferUnavailabilitiesForASupply() throws Exception {
        register("/api/supplies", JARS);

        api.perform(post("/api/supplies/{id}/unavailabilities", "s-1").contentType(MediaType.APPLICATION_JSON).content("""
                        {"period": {"start": "2026-10-01T08:00:00", "end": "2026-10-01T12:00:00"}, "reason": "Spilled"}
                        """))
                .andExpect(status().isNotFound());
    }

    @Test
    void answersBadRequestWhenTheUnavailabilityHasNoReason() throws Exception {
        register("/api/staff", DIVER);

        api.perform(post("/api/staff/{id}/unavailabilities", "p-1").contentType(MediaType.APPLICATION_JSON).content("""
                        {"period": {"start": "2026-10-01T08:00:00", "end": "2026-10-01T12:00:00"}}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("reason is required"));
    }

    private ResultActions register(String resource, String body) throws Exception {
        return api.perform(post(resource).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }
}
