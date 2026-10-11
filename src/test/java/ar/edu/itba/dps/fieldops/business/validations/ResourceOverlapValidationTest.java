package ar.edu.itba.dps.fieldops.business.validations;

import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.Reservation;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.resources.Unavailability;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationSubject;
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

    private static final ExpeditionId ANOTHER_EXPEDITION = new ExpeditionId("e-2");

    private final SequentialExpeditionRepository repository = new SequentialExpeditionRepository();
    private final ResourceOverlapValidation validation = new ResourceOverlapValidation();
    private final Zone coast = zone("z-1");
    private final Expedition expedition = expeditionIn(coast, person("p-1")).build(repository);
    private final Person worker = person("p-2");

    private void scheduleWithWorker(String activityId, TimePeriod period) {
        final var item = expedition.schedule(sampling(activityId, coast, hours(0, 24)), period);
        expedition.assignStaff(item, worker);
    }

    @Test
    void flagsAResourceAssignedToTwoActivitiesAtTheSameTime() {
        scheduleWithWorker("a-1", hours(9, 11));
        scheduleWithWorker("a-2", hours(10, 12));

        final var expected = ValidationResult.critical(ResourceOverlapValidation.ID, ValidationSubject.of(new ActivityId("a-1"), new ResourceId("p-2")), "p-2 is assigned to Sampling a-1 and Sampling a-2 at the same time");
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
        worker.reserve(new Reservation(ANOTHER_EXPEDITION, hours(10, 11)));
        scheduleWithWorker("a-1", hours(9, 11));

        final var expected = ValidationResult.critical(ResourceOverlapValidation.ID, ValidationSubject.of(new ActivityId("a-1"), new ResourceId("p-2")), "p-2 is not available during Sampling a-1");
        assertEquals(List.of(expected), validation.validate(expedition));
    }

    @Test
    void flagsAResourceThatIsUnavailable() {
        worker.markUnavailable(new Unavailability(hours(10, 11), "Medical leave"));
        scheduleWithWorker("a-1", hours(9, 11));

        final var expected = ValidationResult.critical(ResourceOverlapValidation.ID, ValidationSubject.of(new ActivityId("a-1"), new ResourceId("p-2")), "p-2 is not available during Sampling a-1");
        assertEquals(List.of(expected), validation.validate(expedition));
    }
}
