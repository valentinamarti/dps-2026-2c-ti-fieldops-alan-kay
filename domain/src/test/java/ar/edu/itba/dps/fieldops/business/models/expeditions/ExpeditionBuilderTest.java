package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.exceptions.InvalidScheduleException;
import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.completeAssignments;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.activity;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.parallel;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.sequential;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpeditionBuilderTest {

    private static final ExpeditionRestrictions RESTRICTIONS = new ExpeditionRestrictions(10, RiskLevel.MEDIUM);

    private final SequentialExpeditionRepository repository = new SequentialExpeditionRepository();
    private final Zone coast = zone("z-1");
    private final Person leader = person("p-1");

    @Test
    void buildsADraftWithTheGivenData() {
        final var expedition = expeditionIn(coast, leader).build(repository);

        assertEquals(ExpeditionStatus.DRAFT, expedition.getStatus());
        assertEquals(List.of("Map the coastal reef"), expedition.getObjectives());
        assertEquals(Set.of(coast), expedition.getZones());
        assertEquals(Set.of(leader), expedition.getResponsibles());
        assertTrue(expedition.getItinerary().getItems().isEmpty());
    }

    @Test
    void schedulesTheActivitiesAddedToIt() {
        final var survey = sampling("a-1", coast, hours(8, 18));

        final var expedition = expeditionIn(coast, leader).addActivity(survey, hours(9, 11)).build(repository);

        assertTrue(expedition.getItinerary().itemFor(survey).isPresent());
    }

    @Test
    void appliesTheItineraryRulesToTheActivitiesAddedToIt() {
        final var morningSurvey = sampling("a-1", coast, hours(8, 12));

        assertThrows(InvalidScheduleException.class,
                () -> expeditionIn(coast, leader).addActivity(morningSurvey, hours(11, 13)).build(repository));
    }

    @Test
    void schedulesTheBlocksAddedToIt() {
        final var dive = sampling("a-1", coast, hours(8, 18));
        final var transect = sampling("a-2", coast, hours(8, 18));

        final var expedition = expeditionIn(coast, leader)
                .addNode(parallel(activity(dive, hours(9, 11)), activity(transect, hours(9, 11))))
                .build(repository);

        final var itinerary = expedition.getItinerary();
        assertEquals(1, itinerary.getNodes().size());
        assertTrue(itinerary.runInParallel(itinerary.itemFor(dive).orElseThrow(), itinerary.itemFor(transect).orElseThrow()));
    }

    @Test
    void appliesTheBlockRulesToTheBlocksAddedToIt() {
        final var dive = sampling("a-1", coast, hours(8, 18));
        final var transect = sampling("a-2", coast, hours(8, 18));
        final var overlapping = sequential(activity(dive, hours(9, 11)), activity(transect, hours(10, 12)));

        assertThrows(InvalidScheduleException.class, () -> expeditionIn(coast, leader).addNode(overlapping).build(repository));
    }

    @Test
    void keepsAsClosedTheFinishedItemsAddedToIt() {
        final var survey = sampling("a-1", coast, hours(8, 18));
        final var executed = expeditionIn(coast, leader).addActivity(survey, hours(9, 11)).build(repository);
        final var done = executed.getItinerary().itemFor(survey).orElseThrow();
        completeAssignments(executed);
        executed.submitForReview();
        executed.approve(List.of(), List.of());
        executed.start();
        executed.startActivity(done, LocalDateTime.of(2026, 10, 1, 9, 0));
        executed.finishActivity(done, LocalDateTime.of(2026, 10, 1, 11, 0), "Samples taken");

        final var expedition = expeditionIn(coast, leader).addFinishedItem(done).build(repository);

        final var kept = expedition.getItinerary().itemFor(survey).orElseThrow();
        assertTrue(kept.isFinished());
        assertEquals(done.getAssignedStaff(), kept.getAssignedStaff());
        assertTrue(expedition.getItinerary().pendingItems().isEmpty());
    }

    @Test
    void rejectsABuildWithoutRepository() {
        assertThrows(NullPointerException.class, () -> expeditionIn(coast, leader).build(null));
    }

    @Test
    void twoBuildsWithTheSameRepositoryGetDifferentIds() {
        final var first = expeditionIn(coast, leader).build(repository);
        final var second = expeditionIn(coast, leader).build(repository);

        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void takesTheIdFromTheRepository() {
        final var expedition = expeditionIn(coast, leader).build(repository);

        assertEquals(new ExpeditionId("e-1"), expedition.getId());
    }

    @Test
    void rejectsAnExpeditionWithoutNamePeriodOrRestrictions() {
        assertThrows(IllegalArgumentException.class, () -> expeditionIn(coast, leader).withName(" ").build(repository));
        assertThrows(NullPointerException.class, () -> expeditionIn(coast, leader).withPeriod(null).build(repository));
        assertThrows(NullPointerException.class, () -> expeditionIn(coast, leader).withRestrictions(null).build(repository));
    }

    @Test
    void rejectsAnExpeditionWithoutObjectivesZonesOrResponsibles() {
        final var withoutObjectives = new ExpeditionBuilder().withName("Survey").withPeriod(hours(0, 24))
                .withRestrictions(RESTRICTIONS).addZone(coast).addResponsible(leader);
        final var withoutZones = new ExpeditionBuilder().withName("Survey").withPeriod(hours(0, 24))
                .withRestrictions(RESTRICTIONS).addObjective("Map the reef").addResponsible(leader);
        final var withoutResponsibles = new ExpeditionBuilder().withName("Survey").withPeriod(hours(0, 24))
                .withRestrictions(RESTRICTIONS).addObjective("Map the reef").addZone(coast);

        assertThrows(IllegalArgumentException.class, () -> withoutObjectives.build(repository));
        assertThrows(IllegalArgumentException.class, () -> withoutZones.build(repository));
        assertThrows(IllegalArgumentException.class, () -> withoutResponsibles.build(repository));
    }
}