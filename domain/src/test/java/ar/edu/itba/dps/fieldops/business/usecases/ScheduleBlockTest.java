package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.InvalidScheduleException;
import ar.edu.itba.dps.fieldops.business.exceptions.NotFoundException;
import ar.edu.itba.dps.fieldops.business.exceptions.UnknownActivityTypeException;
import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.interfaces.itinerary.ItineraryNode;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;
import ar.edu.itba.dps.fieldops.business.rules.ActivityTypeRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.NIGHT_OPERATIONS;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.nightPermit;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScheduleBlockTest {
    private static final ReusableRequirement LIGHTING = new ReusableRequirement(new ResourceCategory("Lighting"), 1);
    private static final Duration SAMPLING = Duration.ofMinutes(120);
    private static final Duration TRANSFER = Duration.ofMinutes(50);
    private static final Duration DRONE_SURVEY = Duration.ofMinutes(90);

    private final ExpeditionRepository repository = mock(ExpeditionRepository.class);
    private final ZoneRepository zones = mock(ZoneRepository.class);
    private final Zone coast = zone("z-1");
    private final Expedition expedition = expeditionIn(coast, person("p-1")).build(new SequentialExpeditionRepository());

    private final ScheduleBlock scheduleBlock =
            new ScheduleBlock(repository, zones, ActivityTypeRegistry.withStandardTypes());
    private final ScheduleActivity scheduleActivity =
            new ScheduleActivity(repository, zones, ActivityTypeRegistry.withStandardTypes());

    ScheduleBlockTest() {
        when(repository.findById(expedition.getId())).thenReturn(Optional.of(expedition));
        when(repository.findById(new ExpeditionId("e-404"))).thenReturn(Optional.empty());
        when(zones.findById("z-1")).thenReturn(Optional.of(coast));
        when(zones.findById("z-404")).thenReturn(Optional.empty());
    }

    @Test
    void buildsAParallelBlockInsideASequentialOneFromTheTreeOfTheCommand() {
        final var block = schedule(PlannedNodeCommand.sequential(
                sampling("a-1", 8, 10),
                PlannedNodeCommand.parallel(
                        transfer("a-2", 10, 11),
                        droneSurvey("a-3", 10, 12))));

        final var itinerary = expedition.getItinerary();
        assertEquals(List.of(block), itinerary.getNodes());
        assertEquals(List.of(item("a-1"), item("a-2"), item("a-3")), itinerary.getItems());
        assertEquals(hours(8, 12), block.getScheduledPeriod());
        assertEquals(SAMPLING.plus(DRONE_SURVEY), block.estimatedDuration());
        assertNotEquals(SAMPLING.plus(TRANSFER).plus(DRONE_SURVEY), block.estimatedDuration());
        assertTrue(itinerary.runInParallel(item("a-2"), item("a-3")));
        assertFalse(itinerary.runInParallel(item("a-1"), item("a-2")));
    }

    @Test
    void aLeafOfABlockCanBeAskedForAsANightActivity() {
        schedule(PlannedNodeCommand.parallel(nightSampling("a-1", 20, 22), sampling("a-2", 20, 22)));

        final var night = item("a-1").getActivity();
        assertEquals(RiskLevel.MEDIUM, night.estimatedRisk());
        assertEquals(Set.of(NIGHT_OPERATIONS), night.requiredCertificationsForAllStaff());
        assertEquals(Set.of(nightPermit(coast)), night.requiredPermits());
        assertTrue(night.requiredEquipment().contains(LIGHTING));
        assertTrue(item("a-2").getActivity().requiredPermits().isEmpty());
    }

    @Test
    void rejectsAnEmptyBlock() {
        assertThrows(InvalidScheduleException.class, () -> schedule(PlannedNodeCommand.sequential()));
        assertThrows(InvalidScheduleException.class, () -> schedule(PlannedNodeCommand.parallel()));
        assertThrows(InvalidScheduleException.class, () -> schedule(PlannedNodeCommand.sequential(
                sampling("a-1", 8, 10), PlannedNodeCommand.parallel())));
    }

    @Test
    void rejectsABlockWithAChildOutsideTheExpeditionPeriod() {
        final var nightShift = PlannedNodeCommand.activity(new PlannedActivityCommand(new ActivityId("a-2"),
                "Activity a-2", hours(20, 30), hours(23, 25), "z-1", List.of(), "Sample collection",
                Map.of("sampleCount", 3), false));

        assertThrows(InvalidScheduleException.class,
                () -> schedule(PlannedNodeCommand.parallel(sampling("a-1", 9, 11), nightShift)));
    }

    @Test
    void rejectsASequentialBlockWhoseChildrenOverlap() {
        assertThrows(InvalidScheduleException.class, () -> schedule(PlannedNodeCommand.sequential(
                sampling("a-1", 9, 11), sampling("a-2", 10, 12))));
    }

    @Test
    void rejectsADependencyBetweenTwoBranchesOfTheSameParallelBlock() {
        assertThrows(InvalidScheduleException.class, () -> schedule(PlannedNodeCommand.parallel(
                sampling("a-1", 9, 11), samplingAfter("a-2", 11, 13, "a-1"))));
    }

    @Test
    void aLeafCanDependOnAnEarlierLeafOfTheSameBlock() {
        schedule(PlannedNodeCommand.sequential(
                sampling("a-1", 8, 10),
                PlannedNodeCommand.parallel(
                        samplingAfter("a-2", 10, 12, "a-1"),
                        samplingAfter("a-3", 10, 12, "a-1"))));

        assertEquals(List.of(item("a-1").getActivity()), item("a-2").getActivity().getDependencies());
        assertEquals(List.of(item("a-2"), item("a-3")), expedition.getItinerary().dependentsOf(item("a-1")));
    }

    @Test
    void aLeafCannotDependOnALaterLeafOfTheSameBlock() {
        final var block = PlannedNodeCommand.sequential(
                samplingAfter("a-1", 8, 10, "a-2"),
                sampling("a-2", 10, 12));

        final var failure = assertThrows(NotFoundException.class, () -> schedule(block));
        assertEquals("There is no scheduled activity with id a-2", failure.getMessage());
    }

    @Test
    void aLeafCanDependOnAnActivityThatIsAlreadyScheduled() {
        scheduleActivity.schedule(new ScheduleActivityCommand(expedition.getId(), sampleCollection("a-1", 8, 10)));

        schedule(PlannedNodeCommand.parallel(samplingAfter("a-2", 10, 12, "a-1")));

        assertEquals(List.of(item("a-1").getActivity()), item("a-2").getActivity().getDependencies());
    }

    @Test
    void aLeafWithADependencyNobodyScheduledFails() {
        final var block = PlannedNodeCommand.parallel(samplingAfter("a-1", 10, 12, "a-404"));

        assertThrows(NotFoundException.class, () -> schedule(block));
    }

    @Test
    void rejectsAKindOfBlockThatIsNeitherSequentialNorParallel() {
        final var children = List.of(sampling("a-1", 9, 11));

        final var failure = assertThrows(InvalidScheduleException.class,
                () -> PlannedNodeCommand.block("Spiral", children));
        assertEquals("there is no kind of block named spiral: a block is sequential or parallel",
                failure.getMessage());
    }

    @Test
    void theNameOfTheKindOfBlockIsReadTheSameWayNoMatterHowItIsWritten() {
        final var block = schedule(PlannedNodeCommand.block(" Parallel ",
                List.of(transfer("a-1", 9, 11), droneSurvey("a-2", 9, 12))));

        assertEquals(DRONE_SURVEY, block.estimatedDuration());
        assertTrue(expedition.getItinerary().runInParallel(item("a-1"), item("a-2")));
    }

    @Test
    void aLeafOfATypeThatIsNotRegisteredFails() {
        final var block = PlannedNodeCommand.parallel(PlannedNodeCommand.activity(new PlannedActivityCommand(
                new ActivityId("a-1"), "Activity a-1", hours(0, 24), hours(9, 11), "z-1", List.of(),
                "Spelunking", Map.of(), false)));

        assertThrows(UnknownActivityTypeException.class, () -> schedule(block));
    }

    @Test
    void aLeafInAZoneThatIsNotRegisteredFails() {
        final var block = PlannedNodeCommand.parallel(PlannedNodeCommand.activity(new PlannedActivityCommand(
                new ActivityId("a-1"), "Activity a-1", hours(0, 24), hours(9, 11), "z-404", List.of(),
                "Sample collection", Map.of("sampleCount", 3), false)));

        final var failure = assertThrows(NotFoundException.class, () -> schedule(block));
        assertEquals("There is no zone with id z-404", failure.getMessage());
    }

    @Test
    void schedulingABlockInAnUnknownExpeditionFails() {
        final var command = new ScheduleBlockCommand(new ExpeditionId("e-404"),
                PlannedNodeCommand.parallel(sampling("a-1", 9, 11)));

        assertThrows(NotFoundException.class, () -> scheduleBlock.schedule(command));
    }

    @Test
    void aRejectedBlockLeavesTheItineraryUntouched() {
        assertThrows(InvalidScheduleException.class, () -> schedule(PlannedNodeCommand.sequential(
                sampling("a-1", 9, 11), sampling("a-2", 10, 12))));

        assertTrue(expedition.getItinerary().getNodes().isEmpty());
    }

    private ItineraryNode schedule(PlannedNodeCommand block) {
        return scheduleBlock.schedule(new ScheduleBlockCommand(expedition.getId(), block));
    }

    private ItineraryItem item(String activityId) {
        return expedition.getItinerary().itemFor(new ActivityId(activityId)).orElseThrow();
    }

    private PlannedNodeCommand sampling(String id, int fromHour, int toHour) {
        return PlannedNodeCommand.activity(sampleCollection(id, fromHour, toHour));
    }

    private PlannedNodeCommand samplingAfter(String id, int fromHour, int toHour, String... dependencyIds) {
        return PlannedNodeCommand.activity(description(id, fromHour, toHour, "Sample collection",
                Map.of("sampleCount", 3), false, Stream.of(dependencyIds).map(ActivityId::new).toList()));
    }

    private PlannedNodeCommand nightSampling(String id, int fromHour, int toHour) {
        return PlannedNodeCommand.activity(description(id, fromHour, toHour, "Sample collection",
                Map.of("sampleCount", 3), true, List.of()));
    }

    private PlannedNodeCommand transfer(String id, int fromHour, int toHour) {
        return PlannedNodeCommand.activity(description(id, fromHour, toHour, "Transfer",
                Map.of("distanceKilometers", 10), false, List.of()));
    }

    private PlannedNodeCommand droneSurvey(String id, int fromHour, int toHour) {
        return PlannedNodeCommand.activity(description(id, fromHour, toHour, "Drone survey",
                Map.of("areaHectares", 20), false, List.of()));
    }

    private PlannedActivityCommand sampleCollection(String id, int fromHour, int toHour) {
        return description(id, fromHour, toHour, "Sample collection", Map.of("sampleCount", 3), false, List.of());
    }

    private PlannedActivityCommand description(String id, int fromHour, int toHour, String typeName,
                                               Map<String, Integer> parameters, boolean atNight,
                                               List<ActivityId> dependencyIds) {
        return new PlannedActivityCommand(new ActivityId(id), "Activity " + id, hours(0, 24),
                hours(fromHour, toHour), "z-1", dependencyIds, typeName, parameters, atNight);
    }
}
