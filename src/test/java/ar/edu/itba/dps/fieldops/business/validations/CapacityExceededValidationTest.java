package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionRestrictions;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapacityExceededValidationTest {

    private final CapacityExceededValidation validation = new CapacityExceededValidation();
    private final Zone coast = zone("z-1");
    private final Expedition expedition = expeditionIn(coast, person("p-1"))
            .withRestrictions(new ExpeditionRestrictions(1, RiskLevel.MEDIUM))
            .build();

    private ItineraryItem scheduleSampling(String activityId, TimePeriod period) {
        return expedition.schedule(sampling(activityId, coast, hours(0, 24)), period);
    }

    @Test
    void flagsMoreParticipantsThanTheExpeditionAllows() {
        expedition.assignStaff(scheduleSampling("a-1", hours(9, 11)), person("p-2"));
        expedition.assignStaff(scheduleSampling("a-2", hours(12, 14)), person("p-3"));

        final var expected = ValidationResult.critical("The expedition has 2 participants but allows at most 1");
        assertEquals(List.of(expected), validation.validate(expedition));
    }

    @Test
    void countsAPersonInSeveralActivitiesOnlyOnce() {
        final var worker = person("p-2");
        expedition.assignStaff(scheduleSampling("a-1", hours(9, 11)), worker);
        expedition.assignStaff(scheduleSampling("a-2", hours(12, 14)), worker);

        assertTrue(validation.validate(expedition).isEmpty());
    }
}
