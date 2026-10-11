package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.InvalidStatusTransitionException;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.replanning.Cancellation;
import ar.edu.itba.dps.fieldops.business.replanning.ExpeditionReplanner;
import ar.edu.itba.dps.fieldops.business.services.ResourceReservations;
import ar.edu.itba.dps.fieldops.providers.InMemoryExpeditionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.assignContainers;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.assignCooler;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApproveAlternativeTest {

    private final InMemoryExpeditionRepository repository = new InMemoryExpeditionRepository();
    private final ApproveExpedition approveExpedition =
            new ApproveExpedition(List.of(), new ResourceReservations(), repository);
    private final ReplanExpedition replanExpedition = new ReplanExpedition(repository, new ExpeditionReplanner(repository));
    private final Zone coast = zone("z-1");
    private final Person keptWorker = person("p-a-1");
    private final Person affectedWorker = person("p-a-2");
    private final Expedition original = new CreateExpedition(repository).create(expeditionIn(coast, person("p-1")));
    private final ItineraryItem keptItem = scheduleEquipped("a-1", hours(9, 11), keptWorker);
    private final ItineraryItem affectedItem = scheduleEquipped("a-2", hours(13, 15), affectedWorker);

    private ItineraryItem scheduleEquipped(String activityId, TimePeriod period, Person worker) {
        final var item = original.schedule(sampling(activityId, coast, hours(0, 24)), period);
        original.assignStaff(item, worker);
        assignCooler(original, item);
        assignContainers(original, item);
        return item;
    }

    private Expedition approvedAlternative() {
        original.submitForReview();
        approveExpedition.approve(original, List.of());
        final var alternative = replanExpedition.replan(original, affectedItem, new Cancellation());
        alternative.submitForReview();
        return alternative;
    }

    @Test
    void anAlternativeCanReuseTheResourcesOfTheExpeditionItReplaces() {
        final var alternative = approvedAlternative();

        approveExpedition.approve(alternative, List.of());

        assertEquals(ExpeditionStatus.APPROVED, alternative.getStatus());
        assertEquals(ExpeditionStatus.REPLACED, original.getStatus());
        assertEquals(Optional.of(original.getId()), alternative.getReplacedExpedition());
        assertEquals(Optional.of(alternative.getId()), original.getReplacement());
        assertFalse(keptWorker.isAvailableDuring(hours(9, 11)));
        assertTrue(affectedWorker.isAvailableDuring(hours(13, 15)));
    }

    @Test
    void anOriginalThatWasAlreadyReplacedCannotBeReplacedAgain() {
        final var first = approvedAlternative();
        final var second = replanExpedition.replan(original, keptItem, new Cancellation());
        second.submitForReview();
        approveExpedition.approve(first, List.of());

        assertThrows(InvalidStatusTransitionException.class, () -> approveExpedition.approve(second, List.of()));
        assertEquals(ExpeditionStatus.IN_REVIEW, second.getStatus());
        assertEquals(Optional.of(first.getId()), original.getReplacement());
    }

    @Test
    void anAlternativeThatIsNeverApprovedLeavesTheOriginalAsItWas() {
        approvedAlternative();

        assertEquals(ExpeditionStatus.APPROVED, original.getStatus());
        assertTrue(original.getReplacement().isEmpty());
        assertFalse(affectedWorker.isAvailableDuring(hours(13, 15)));
    }
}
