package ar.edu.itba.dps.fieldops.business.models.resources;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CertificationTest {

    @Test
    void spellingVariantsOfTheSameNameAreTheSameCertification() {
        final var certification = new Certification("first aid");

        assertEquals(certification, new Certification("First Aid"));
        assertEquals(certification, new Certification("FIRST AID"));
        assertEquals(certification, new Certification(" first aid  "));
        assertEquals(certification.hashCode(), new Certification("First aid ").hashCode());
    }

    @Test
    void keepsTheNameStrippedAndInLowerCase() {
        assertEquals("first aid", new Certification("  First Aid ").name());
    }

    @Test
    void differentNamesAreDifferentCertifications() {
        assertNotEquals(new Certification("diving"), new Certification("buceo"));
    }

    @Test
    void aSetHoldingOneSpellingIncludesTheOthers() {
        assertTrue(new Certifications(Set.of(new Certification("Diving"))).includes(new Certification("diving ")));
    }

    @Test
    void rejectsABlankOrNullName() {
        assertThrows(IllegalArgumentException.class, () -> new Certification(" "));
        assertThrows(IllegalArgumentException.class, () -> new Certification(null));
    }
}
