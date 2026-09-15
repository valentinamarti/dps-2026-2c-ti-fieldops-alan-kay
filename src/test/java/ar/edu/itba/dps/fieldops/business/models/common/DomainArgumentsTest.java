package ar.edu.itba.dps.fieldops.business.models.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomainArgumentsTest {

    @Test
    void textIsReturnedUnchangedWhenItHasContent() {
        assertEquals("p-1", DomainArguments.requireText("p-1", "id"));
    }

    @Test
    void nullTextIsRejectedNamingTheField() {
        final var error = assertThrows(IllegalArgumentException.class,
                () -> DomainArguments.requireText(null, "id"));

        assertEquals("id is required", error.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", "\n"})
    void blankTextIsRejectedNamingTheField(String blank) {
        final var error = assertThrows(IllegalArgumentException.class,
                () -> DomainArguments.requireText(blank, "name"));

        assertEquals("name cannot be blank", error.getMessage());
    }

    @Test
    void nullSetIsRejectedNamingTheFieldInsteadOfFailingInsideCopyOf() {
        final var error = assertThrows(NullPointerException.class,
                () -> DomainArguments.requireSet(null, "certifications"));

        assertEquals("certifications is required", error.getMessage());
    }

    @Test
    void nullListIsRejectedNamingTheField() {
        final var error = assertThrows(NullPointerException.class,
                () -> DomainArguments.requireList(null, "dependencies"));

        assertEquals("dependencies is required", error.getMessage());
    }

    @Test
    void setIsCopiedSoLaterChangesToTheSourceDoNotLeakIn() {
        final var source = new HashSet<>(Set.of("a"));

        final var copy = DomainArguments.requireSet(source, "values");
        source.add("b");

        assertEquals(Set.of("a"), copy);
    }

    @Test
    void listIsCopiedSoLaterChangesToTheSourceDoNotLeakIn() {
        final var source = new ArrayList<>(List.of("a"));

        final var copy = DomainArguments.requireList(source, "values");
        source.add("b");

        assertEquals(List.of("a"), copy);
    }

    @Test
    void copiesAreUnmodifiable() {
        assertThrows(UnsupportedOperationException.class,
                () -> DomainArguments.requireSet(Set.of("a"), "values").add("b"));
        assertThrows(UnsupportedOperationException.class,
                () -> DomainArguments.requireList(List.of("a"), "values").add("b"));
    }
}