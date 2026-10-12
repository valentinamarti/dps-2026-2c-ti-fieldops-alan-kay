package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.interfaces.activities.ActivityType;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityTypeName;
import ar.edu.itba.dps.fieldops.business.rules.ActivityTypeRegistry;
import ar.edu.itba.dps.fieldops.business.rules.DivingRules;
import ar.edu.itba.dps.fieldops.business.rules.TransferRules;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryActivityTypesTest {

    @Test
    void listsTheAvailableTypesWithTheirParameters() {
        final var types = new QueryActivityTypes(new ActivityTypeRegistry(List.of(DivingRules.TYPE, TransferRules.TYPE))).list();

        assertEquals(List.of(new ActivityTypeName("diving"), new ActivityTypeName("transfer")),
                types.stream().map(ActivityType::name).toList());
        assertEquals(List.of(List.of("maxDepthMeters"), List.of("distanceKilometers")),
                types.stream().map(ActivityType::parameterNames).toList());
    }

    @Test
    void listsTheSixTypesTheDomainDefines() {
        assertEquals(6, new QueryActivityTypes(ActivityTypeRegistry.withStandardTypes()).list().size());
    }

    @Test
    void needsARegistry() {
        assertThrows(NullPointerException.class, () -> new QueryActivityTypes(null));
    }
}
