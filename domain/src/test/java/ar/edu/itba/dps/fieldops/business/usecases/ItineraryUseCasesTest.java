package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.exceptions.NotFoundException;
import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.instrument;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.supply;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static ar.edu.itba.dps.fieldops.business.models.common.MeasurementUnit.UNITS;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.activity;
import static ar.edu.itba.dps.fieldops.business.models.expeditions.PlannedNode.parallel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ItineraryUseCasesTest {

    private static final ActivityId SAMPLING = new ActivityId("a-1");

    private final ExpeditionRepository repository = mock(ExpeditionRepository.class);
    private final ResourceCatalog catalog = new ResourceCatalog();
    private final Zone coast = zone("z-1");
    private final Expedition expedition = expeditionIn(coast, person("p-1")).build(new SequentialExpeditionRepository());

    private final ScheduleActivity scheduleActivity = new ScheduleActivity(repository);
    private final ScheduleBlock scheduleBlock = new ScheduleBlock(repository);
    private final AssignResources assignResources = new AssignResources(repository, catalog);
    private final FindAssignmentCandidates candidates = new FindAssignmentCandidates(repository, catalog);

    ItineraryUseCasesTest() {
        when(repository.findById(expedition.getId())).thenReturn(Optional.of(expedition));
        when(repository.findById(new ExpeditionId("e-404"))).thenReturn(Optional.empty());
    }

    private ItineraryItem scheduleSampling() {
        return scheduleActivity.schedule(new ScheduleActivityCommand(expedition.getId(), sampling("a-1", coast, hours(0, 24)), hours(9, 11)));
    }

    @Test
    void schedulesTheActivityInTheExpeditionTheIdPointsTo() {
        final var item = scheduleSampling();

        assertSame(item, expedition.getItinerary().itemFor(SAMPLING).orElseThrow());
        assertEquals(hours(9, 11), item.getScheduledPeriod());
    }

    @Test
    void schedulingInAnUnknownExpeditionFails() {
        final var command = new ScheduleActivityCommand(new ExpeditionId("e-404"), sampling("a-1", coast, hours(0, 24)), hours(9, 11));

        assertThrows(NotFoundException.class, () -> scheduleActivity.schedule(command));
    }

    @Test
    void schedulesTheBlockInTheExpeditionTheIdPointsTo() {
        final var dive = sampling("a-1", coast, hours(0, 24));
        final var transect = sampling("a-2", coast, hours(0, 24));

        final var block = scheduleBlock.schedule(new ScheduleBlockCommand(expedition.getId(),
                parallel(activity(dive, hours(9, 11)), activity(transect, hours(9, 12)))));

        final var itinerary = expedition.getItinerary();
        assertEquals(List.of(block), itinerary.getNodes());
        assertEquals(hours(9, 12), block.getScheduledPeriod());
        assertTrue(itinerary.runInParallel(itinerary.itemFor(dive).orElseThrow(), itinerary.itemFor(transect).orElseThrow()));
    }

    @Test
    void schedulingABlockInAnUnknownExpeditionFails() {
        final var command = new ScheduleBlockCommand(new ExpeditionId("e-404"),
                parallel(activity(sampling("a-1", coast, hours(0, 24)), hours(9, 11))));

        assertThrows(NotFoundException.class, () -> scheduleBlock.schedule(command));
    }

    @Test
    void assignsTheStaffEquipmentAndSupplyTheIdsPointTo() {
        final var worker = person("p-2");
        final var cooler = instrument("i-1", "Cooler");
        final var jars = supply("d-1", "Sample container", Quantity.of("10", UNITS));
        catalog.addStaff(worker);
        catalog.addEquipment(cooler);
        catalog.addSupply(jars);
        final var item = scheduleSampling();

        assignResources.assignStaff(expedition.getId(), SAMPLING, new ResourceId("p-2"));
        assignResources.assignEquipment(expedition.getId(), SAMPLING, new ResourceId("i-1"));
        assignResources.assignSupply(expedition.getId(), SAMPLING, new ResourceId("d-1"));

        assertTrue(item.getAssignedStaff().contains(worker));
        assertTrue(item.getAssignedEquipment().contains(cooler));
        assertTrue(item.getAssignedSupplies().contains(jars));
    }

    @Test
    void assigningFailsWhenTheExpeditionTheActivityOrTheResourceDoNotExist() {
        scheduleSampling();

        assertThrows(NotFoundException.class,
                () -> assignResources.assignStaff(new ExpeditionId("e-404"), SAMPLING, new ResourceId("p-2")));
        assertThrows(NotFoundException.class,
                () -> assignResources.assignStaff(expedition.getId(), new ActivityId("a-404"), new ResourceId("p-2")));
        assertThrows(NotFoundException.class,
                () -> assignResources.assignStaff(expedition.getId(), SAMPLING, new ResourceId("p-404")));
        assertThrows(NotFoundException.class,
                () -> assignResources.assignEquipment(expedition.getId(), SAMPLING, new ResourceId("i-404")));
        assertThrows(NotFoundException.class,
                () -> assignResources.assignSupply(expedition.getId(), SAMPLING, new ResourceId("d-404")));
    }

    @Test
    void offersTheCatalogResourcesThatCoverEachRequirementOfTheActivity() {
        final var worker = person("p-2");
        final var cooler = instrument("i-1", "Cooler");
        final var tank = instrument("i-2", "Oxygen tank");
        final var jars = supply("d-1", "Sample container", Quantity.of("10", UNITS));
        catalog.addStaff(worker);
        catalog.addEquipment(cooler);
        catalog.addEquipment(tank);
        catalog.addSupply(jars);
        scheduleSampling();

        final var offer = candidates.candidatesFor(expedition.getId(), SAMPLING);

        assertTrue(offer.staff().stream().allMatch(entry -> entry.candidates().contains(worker)));
        assertTrue(offer.equipment().stream().flatMap(entry -> entry.candidates().stream()).anyMatch(cooler::equals));
        assertTrue(offer.equipment().stream().flatMap(entry -> entry.candidates().stream()).noneMatch(tank::equals));
        assertTrue(offer.supplies().stream().flatMap(entry -> entry.candidates().stream()).anyMatch(jars::equals));
    }

    @Test
    void offeringCandidatesForAnUnknownActivityFails() {
        assertThrows(NotFoundException.class, () -> candidates.candidatesFor(expedition.getId(), new ActivityId("a-404")));
    }
}
