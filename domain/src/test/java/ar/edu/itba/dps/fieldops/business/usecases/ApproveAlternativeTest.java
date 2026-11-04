package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures;
import ar.edu.itba.dps.fieldops.business.fixtures.FakeResourceCatalog;
import ar.edu.itba.dps.fieldops.business.providers.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.exceptions.InvalidStatusTransitionException;
import ar.edu.itba.dps.fieldops.business.interfaces.events.DomainEvent;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.events.ExpeditionApproved;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.replanning.Cancellation;
import ar.edu.itba.dps.fieldops.business.replanning.ExpeditionReplanner;
import ar.edu.itba.dps.fieldops.business.services.PublishingExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.services.ResourceReservations;
import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.APPROVED_AT;
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

    private final SequentialExpeditionRepository repository = new SequentialExpeditionRepository();
    private final ApproveExpedition approveExpedition =
            new ApproveExpedition(List.of(), new ResourceReservations(), repository, new FakeResourceCatalog());
    private final ReplanExpedition replanExpedition = new ReplanExpedition(repository, new ExpeditionReplanner(repository));
    private final Zone coast = zone("z-1");
    private final Person keptWorker = person("p-a-1");
    private final Person affectedWorker = person("p-a-2");
    private final Expedition original = ExpeditionFixtures.createExpeditionOn(repository).create(expeditionIn(coast, person("p-1")));
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
        approveExpedition.approve(original, List.of(), APPROVED_AT);
        final var alternative = replanExpedition.replan(original, affectedItem, new Cancellation());
        alternative.submitForReview();
        return alternative;
    }

    @Test
    void anAlternativeCanReuseTheResourcesOfTheExpeditionItReplaces() {
        final var alternative = approvedAlternative();

        approveExpedition.approve(alternative, List.of(), APPROVED_AT);

        assertEquals(ExpeditionStatus.APPROVED, alternative.getStatus());
        assertEquals(ExpeditionStatus.REPLACED, original.getStatus());
        assertEquals(Optional.of(original.getId()), alternative.getReplacedExpedition());
        assertEquals(Optional.of(alternative.getId()), original.getReplacement());
        assertFalse(keptWorker.isAvailableDuring(hours(9, 11)));
        assertTrue(affectedWorker.isAvailableDuring(hours(13, 15)));
    }

    @Test
    void approvingAnAlternativeThroughThePublishingRepositoryPublishesItsApproval() {
        final var alternative = approvedAlternative();
        final var published = new ArrayList<DomainEvent>();
        final var publishing = new ApproveExpedition(List.of(), new ResourceReservations(),
                new PublishingExpeditionRepository(repository, published::add), new FakeResourceCatalog());

        publishing.approve(alternative, List.of(), APPROVED_AT);

        final var approved = published.stream().filter(ExpeditionApproved.class::isInstance).map(DomainEvent::expeditionId).toList();
        assertTrue(approved.contains(alternative.getId()));
    }

    @Test
    void anOriginalThatWasAlreadyReplacedCannotBeReplacedAgain() {
        final var first = approvedAlternative();
        final var second = replanExpedition.replan(original, keptItem, new Cancellation());
        second.submitForReview();
        approveExpedition.approve(first, List.of(), APPROVED_AT);

        assertThrows(InvalidStatusTransitionException.class, () -> approveExpedition.approve(second, List.of(), APPROVED_AT));
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
