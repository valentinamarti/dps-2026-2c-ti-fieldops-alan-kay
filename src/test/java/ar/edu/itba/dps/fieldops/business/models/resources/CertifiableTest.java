package ar.edu.itba.dps.fieldops.business.models.resources;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Certification rules shared by every {@link Certifiable} resource.
 */
class CertifiableTest {

    private static final Certification DIVING = new Certification("Diving");
    private static final Certification FIRST_AID = new Certification("First aid");

    static Stream<Certifiable> resourcesCertifiedForDiving() {
        return Stream.of(
                new Person("p-1", "Ana", Set.of(DIVING)),
                new Vehicle("v-1", "Boat", "ABC 123", Set.of(DIVING)),
                new Instrument("i-1", "Sonar", Set.of(DIVING))
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
