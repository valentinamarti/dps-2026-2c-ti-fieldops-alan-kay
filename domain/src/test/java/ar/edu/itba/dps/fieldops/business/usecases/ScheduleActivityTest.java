package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.InvalidActivityParametersException;
import ar.edu.itba.dps.fieldops.business.exceptions.NotFoundException;
import ar.edu.itba.dps.fieldops.business.exceptions.UnknownActivityTypeException;
import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
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

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.NIGHT_OPERATIONS;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.nightPermit;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScheduleActivityTest {
    private static final ReusableRequirement LIGHTING = new ReusableRequirement(new ResourceCategory("Lighting"), 1);

    private final ExpeditionRepository repository = mock(ExpeditionRepository.class);
    private final ZoneRepository zones = mock(ZoneRepository.class);
    private final SequentialExpeditionRepository ids = new SequentialExpeditionRepository();
    private final Zone coast = zone("z-1");
    private final Expedition expedition = expeditionIn(coast, person("p-1")).build(ids);

    private final ScheduleActivity scheduleActivity =
            new ScheduleActivity(repository, zones, ActivityTypeRegistry.withStandardTypes());

    ScheduleActivityTest() {
        when(repository.findById(expedition.getId())).thenReturn(Optional.of(expedition));
        when(repository.findById(new ExpeditionId("e-404"))).thenReturn(Optional.empty());
        when(zones.findById("z-1")).thenReturn(Optional.of(coast));
        when(zones.findById("z-404")).thenReturn(Optional.empty());
    }

    @Test
    void resolvesTheZoneAndBuildsTheRulesOfTheTypeTheCommandAsksFor() {
        final var item = scheduleActivity.schedule(sampleCollection("a-1", 3).build());

        assertSame(item, expedition.getItinerary().itemFor(new ActivityId("a-1")).orElseThrow());
        assertEquals(hours(9, 11), item.getScheduledPeriod());
        assertSame(coast, item.getActivity().getZone());
        assertEquals(Duration.ofHours(2), item.estimatedDuration());
        assertEquals(RiskLevel.LOW, item.estimatedRisk());
        assertEquals(3, item.requiredSupplies().getFirst().quantity().amount().intValueExact());
    }

    @Test
    void anotherTypeOfTheRegistryIsAskedForByItsNameWithItsOwnParameter() {
        final var diving = scheduleActivity.schedule(command("a-1")
                .typeName("Diving")
                .parameters(Map.of("maxDepthMeters", 40))
                .build());

        assertEquals(RiskLevel.HIGH, diving.estimatedRisk());
        assertEquals(Duration.ofMinutes(45 + 2 * 40), diving.estimatedDuration());
    }

    @Test
    void resolvesTheDependenciesAmongTheActivitiesAlreadyScheduledInTheSameExpedition() {
        final var first = scheduleActivity.schedule(sampleCollection("a-1", 3).build());

        final var second = scheduleActivity.schedule(sampleCollection("a-2", 3)
                .scheduledPeriod(hours(11, 13))
                .dependencyIds(List.of(new ActivityId("a-1")))
                .build());

        assertEquals(List.of(first.getActivity()), second.getActivity().getDependencies());
        assertTrue(second.getActivity().dependsOn(first.getActivity()));
        assertEquals(List.of(second), expedition.getItinerary().dependentsOf(first));
    }

    @Test
    void theNightIndicatorWrapsTheRulesOfTheTypeWithWhatANightOperationDemands() {
        final var night = scheduleActivity.schedule(sampleCollection("a-1", 3)
                .scheduledPeriod(hours(20, 22))
                .atNight(true)
                .build()).getActivity();

        assertEquals(RiskLevel.MEDIUM, night.estimatedRisk());
        assertEquals(Set.of(NIGHT_OPERATIONS), night.requiredCertificationsForAllStaff());
        assertEquals(Set.of(nightPermit(coast)), night.requiredPermits());
        assertTrue(night.requiredEquipment().contains(LIGHTING));
        assertEquals(Duration.ofHours(2), night.estimatedDuration());
    }

    @Test
    void theSameActivityWithoutTheNightIndicatorDemandsNothingOfANightOperation() {
        final var day = scheduleActivity.schedule(sampleCollection("a-1", 3).build()).getActivity();

        assertTrue(day.requiredCertificationsForAllStaff().isEmpty());
        assertTrue(day.requiredPermits().isEmpty());
        assertFalse(day.requiredEquipment().contains(LIGHTING));
    }

    @Test
    void schedulingATypeThatIsNotRegisteredFails() {
        final var command = sampleCollection("a-1", 3).typeName("Spelunking").build();

        final var failure = assertThrows(UnknownActivityTypeException.class, () -> scheduleActivity.schedule(command));
        assertTrue(failure.getMessage().contains("sample collection"));
    }

    @Test
    void schedulingWithoutTheParameterTheTypeNeedsFails() {
        final var command = command("a-1").parameters(Map.of()).build();

        final var failure = assertThrows(InvalidActivityParametersException.class,
                () -> scheduleActivity.schedule(command));
        assertTrue(failure.getMessage().contains("the parameter sampleCount is required"));
    }

    @Test
    void schedulingWithAParameterTheTypeRejectsFails() {
        final var command = sampleCollection("a-1", 0).build();

        final var failure = assertThrows(InvalidActivityParametersException.class,
                () -> scheduleActivity.schedule(command));
        assertTrue(failure.getMessage().contains("sample count must be positive"));
    }

    @Test
    void schedulingInAZoneThatIsNotRegisteredFails() {
        final var command = sampleCollection("a-1", 3).zoneId("z-404").build();

        final var failure = assertThrows(NotFoundException.class, () -> scheduleActivity.schedule(command));
        assertEquals("There is no zone with id z-404", failure.getMessage());
    }

    @Test
    void schedulingWithADependencyThatNobodyScheduledFails() {
        final var command = sampleCollection("a-1", 3).dependencyIds(List.of(new ActivityId("a-404"))).build();

        final var failure = assertThrows(NotFoundException.class, () -> scheduleActivity.schedule(command));
        assertEquals("There is no scheduled activity with id a-404", failure.getMessage());
    }

    @Test
    void theDependencyHasToBeScheduledInTheSameExpedition() {
        final var other = expeditionIn(coast, person("p-1")).build(ids);
        when(repository.findById(other.getId())).thenReturn(Optional.of(other));
        scheduleActivity.schedule(sampleCollection("a-1", 3).expeditionId(other.getId()).build());

        final var command = sampleCollection("a-2", 3).dependencyIds(List.of(new ActivityId("a-1"))).build();

        assertThrows(NotFoundException.class, () -> scheduleActivity.schedule(command));
    }

    @Test
    void schedulingInAnUnknownExpeditionFails() {
        final var command = sampleCollection("a-1", 3).expeditionId(new ExpeditionId("e-404")).build();

        assertThrows(NotFoundException.class, () -> scheduleActivity.schedule(command));
    }

    private CommandBuilder sampleCollection(String id, int sampleCount) {
        return command(id).parameters(Map.of("sampleCount", sampleCount));
    }

    private CommandBuilder command(String id) {
        return new CommandBuilder(expedition.getId(), id);
    }

    private static final class CommandBuilder {
        private final ActivityId activityId;
        private ExpeditionId expeditionId;
        private TimePeriod scheduledPeriod = hours(9, 11);
        private String zoneId = "z-1";
        private List<ActivityId> dependencyIds = List.of();
        private String typeName = "Sample collection";
        private Map<String, Integer> parameters = Map.of();
        private boolean atNight;

        private CommandBuilder(ExpeditionId expeditionId, String activityId) {
            this.expeditionId = expeditionId;
            this.activityId = new ActivityId(activityId);
        }

        private CommandBuilder expeditionId(ExpeditionId expeditionId) {
            this.expeditionId = expeditionId;
            return this;
        }

        private CommandBuilder scheduledPeriod(TimePeriod scheduledPeriod) {
            this.scheduledPeriod = scheduledPeriod;
            return this;
        }

        private CommandBuilder zoneId(String zoneId) {
            this.zoneId = zoneId;
            return this;
        }

        private CommandBuilder dependencyIds(List<ActivityId> dependencyIds) {
            this.dependencyIds = dependencyIds;
            return this;
        }

        private CommandBuilder typeName(String typeName) {
            this.typeName = typeName;
            return this;
        }

        private CommandBuilder parameters(Map<String, Integer> parameters) {
            this.parameters = parameters;
            return this;
        }

        private CommandBuilder atNight(boolean atNight) {
            this.atNight = atNight;
            return this;
        }

        private ScheduleActivityCommand build() {
            return new ScheduleActivityCommand(expeditionId, new PlannedActivityCommand(activityId,
                    "Activity " + activityId, hours(0, 24), scheduledPeriod, zoneId, dependencyIds, typeName,
                    parameters, atNight));
        }
    }
}
