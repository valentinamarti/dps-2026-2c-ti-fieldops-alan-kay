package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
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

class ResourceOverlapValidationTest {

    private final ResourceOverlapValidation validation = new ResourceOverlapValidation();
    private final Zone coast = zone("z-1");
    private final Expedition expedition = expeditionIn(coast, person("p-1")).build();
    private final Person worker = person("p-2");

    private void scheduleWithWorker(String activityId, TimePeriod period) {
        final var item = expedition.schedule(sampling(activityId, coast, hours(0, 24)), period);
        expedition.assignStaff(item, worker);
    }

    @Test
    void flagsAResourceAssignedToTwoActivitiesAtTheSameTime() {
        scheduleWithWorker("a-1", hours(9, 11));
        scheduleWithWorker("a-2", hours(10, 12));

        final var expected = ValidationResult.critical("p-2 is assigned to Sampling a-1 and Sampling a-2 at the same time");
        assertEquals(List.of(expected), validation.validate(expedition));
    }

    @Test
    void acceptsTheSameResourceInActivitiesThatDoNotOverlap() {
        scheduleWithWorker("a-1", hours(9, 11));
        scheduleWithWorker("a-2", hours(11, 13));

        assertTrue(validation.validate(expedition).isEmpty());
    }

    @Test
    void flagsAResourceAlreadyBookedByAnotherExpedition() {
        worker.reserve(hours(10, 11));
        scheduleWithWorker("a-1", hours(9, 11));

        final var expected = ValidationResult.critical("p-2 is already booked during Sampling a-1");
        assertEquals(List.of(expected), validation.validate(expedition));
    }
}