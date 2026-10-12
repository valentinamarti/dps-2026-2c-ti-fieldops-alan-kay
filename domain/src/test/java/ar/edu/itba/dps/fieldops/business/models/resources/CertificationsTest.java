package ar.edu.itba.dps.fieldops.business.models.resources;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CertificationsTest {

    private static final Certification DIVING = new Certification("Diving");
    private static final Certification FIRST_AID = new Certification("First aid");

    @Test
    void includesTheCertificationsItHolds() {
        assertTrue(new Certifications(Set.of(DIVING)).includes(DIVING));
    }

    @Test
    void doesNotIncludeCertificationsItDoesNotHold() {
        assertFalse(new Certifications(Set.of(DIVING)).includes(FIRST_AID));
    }

    @Test
    void anEmptySetHoldsNothing() {
        assertFalse(new Certifications(Set.of()).includes(DIVING));
    }

    @Test
    void twoSetsWithTheSameCertificationsAreEqual() {
        assertEquals(new Certifications(Set.of(DIVING, FIRST_AID)), new Certifications(Set.of(FIRST_AID, DIVING)));
    }

    @Test
    void rejectsANullSet() {
        assertThrows(NullPointerException.class, () -> new Certifications(null));
    }

    @Test
    void copiesTheSetSoLaterChangesDoNotLeakIn() {
        final var source = new HashSet<>(Set.of(DIVING));

        final var certifications = new Certifications(source);
        source.add(FIRST_AID);

        assertFalse(certifications.includes(FIRST_AID));
    }
}