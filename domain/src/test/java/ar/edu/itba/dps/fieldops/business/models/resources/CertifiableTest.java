package ar.edu.itba.dps.fieldops.business.models.resources;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ar.edu.itba.dps.fieldops.business.interfaces.resources.Certifiable;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CertifiableTest {
    private static final Certification DIVING = new Certification("Diving");
    private static final Certification FIRST_AID = new Certification("First aid");
    private static final ResourceCategory BOAT = new ResourceCategory("Boat");
    private static final ResourceCategory SONAR = new ResourceCategory("Sonar");

    static Stream<Certifiable> resourcesCertifiedForDiving() {
        return Stream.of(
                new Person(new ResourceId("p-1"), "Ana", Set.of(DIVING)),
                new Vehicle(new ResourceId("v-1"), "Albatros", BOAT, "ABC 123", Set.of(DIVING)),
                new Instrument(new ResourceId("i-1"), "Sonar 3000", SONAR, Set.of(DIVING))
        );
    }

    @ParameterizedTest
    @MethodSource("resourcesCertifiedForDiving")
    void hasTheCertificationsItWasGiven(Certifiable resource) {
        assertTrue(resource.hasCertification(DIVING));
    }

    @ParameterizedTest
    @MethodSource("resourcesCertifiedForDiving")
    void lacksCertificationsItWasNotGiven(Certifiable resource) {
        assertFalse(resource.hasCertification(FIRST_AID));
    }
}
