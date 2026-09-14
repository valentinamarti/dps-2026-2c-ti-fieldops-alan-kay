package ar.edu.itba.dps.fieldops.business.models.activities;

import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaffRequirementTest {

    private static final Certification DIVING = new Certification("Diving");
    private static final Certification FIRST_AID = new Certification("First aid");

    private final StaffRequirement rescueDivers = new StaffRequirement(2, Set.of(DIVING, FIRST_AID));

    @Test
    void candidateWithEveryRequiredCertificationQualifies() {
        final var candidate = new Person("p-1", "Ana", Set.of(DIVING, FIRST_AID));

        assertTrue(rescueDivers.qualifies(candidate));
    }

    @Test
    void candidateMissingOneRequiredCertificationDoesNotQualify() {
        final var candidate = new Person("p-1", "Ana", Set.of(DIVING));

        assertFalse(rescueDivers.qualifies(candidate));
    }

    @Test
    void requirementWithoutCertificationsAcceptsAnyCandidate() {
        final var anyone = new StaffRequirement(1, Set.of());

        assertTrue(anyone.qualifies(new Person("p-1", "Ana", Set.of())));
    }

    @Test
    void rejectsNonPositiveHeadcount() {
        assertThrows(IllegalArgumentException.class, () -> new StaffRequirement(0, Set.of()));
    }
}
