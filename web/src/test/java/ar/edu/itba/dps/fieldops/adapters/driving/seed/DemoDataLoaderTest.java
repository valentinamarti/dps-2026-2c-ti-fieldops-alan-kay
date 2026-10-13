package ar.edu.itba.dps.fieldops.adapters.driving.seed;

import ar.edu.itba.dps.fieldops.adapters.driven.InMemoryExpeditionRepository;
import ar.edu.itba.dps.fieldops.adapters.driven.InMemoryZoneRepository;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.activities.ReusableRequirement;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ItineraryItem;
import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.providers.ZoneRepository;
import ar.edu.itba.dps.fieldops.business.rules.ActivityTypeRegistry;
import ar.edu.itba.dps.fieldops.business.usecases.AssignResources;
import ar.edu.itba.dps.fieldops.business.usecases.CreateExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.MarkResourceUnavailable;
import ar.edu.itba.dps.fieldops.business.usecases.RegisterResources;
import ar.edu.itba.dps.fieldops.business.usecases.RegisterZone;
import ar.edu.itba.dps.fieldops.business.usecases.ScheduleActivity;
import ar.edu.itba.dps.fieldops.business.usecases.ScheduleBlock;
import ar.edu.itba.dps.fieldops.business.usecases.ValidateExpedition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemoDataLoaderTest {
    private static final List<Scheduled> THE_TEN_ACTIVITIES = List.of(
            new Scheduled("a-01", "Truck", 270, RiskLevel.MEDIUM),
            new Scheduled("a-02", "Multiparameter probe", 90, RiskLevel.LOW),
            new Scheduled("a-03", "Boat", 81, RiskLevel.MEDIUM),
            new Scheduled("a-04", "Drone", 210, RiskLevel.MEDIUM),
            new Scheduled("a-05", "Cooler", 180, RiskLevel.LOW),
            new Scheduled("a-06", "Binoculars", 165, RiskLevel.LOW),
            new Scheduled("a-07", "Multiparameter probe", 60, RiskLevel.LOW),
            new Scheduled("a-08", "Cooler", 140, RiskLevel.LOW),
            new Scheduled("a-09", "Drone", 90, RiskLevel.LOW),
            new Scheduled("a-11", "Truck", 270, RiskLevel.MEDIUM));

    private ExpeditionRepository expeditions;
    private ResourceCatalog catalog;
    private ZoneRepository zones;
    private ValidateExpedition validate;
    private DemoDataLoader loader;

    @BeforeEach
    void wireTheHexagonWithTheInMemoryAdapters() {
        expeditions = new InMemoryExpeditionRepository();
        catalog = new ResourceCatalog();
        zones = new InMemoryZoneRepository();
        validate = new ValidateExpedition(expeditions, List.of());
        final var types = ActivityTypeRegistry.withStandardTypes();
        loader = new DemoDataLoader(
                new RegisterZone(zones),
                new RegisterResources(catalog),
                new MarkResourceUnavailable(catalog),
                new CreateExpedition(expeditions, zones, catalog),
                new ScheduleActivity(expeditions, zones, types),
                new ScheduleBlock(expeditions, zones, types),
                new AssignResources(expeditions, catalog),
                JsonMapper.builder().build());
    }

    @Test
    void loadsTheCatalogAndTheZonesTheExpeditionReferences() {
        loadTheDemo();

        assertThat(catalog.allStaff()).hasSize(8);
        assertThat(catalog.allEquipment()).hasSize(11);
        assertThat(catalog.allSupplies()).hasSize(3);
        assertThat(zones.findAll()).extracting(zone -> zone.getId()).containsExactly("z-coast", "z-wetland");
    }

    @Test
    void marksTheSpareDroneUnavailableWhileItIsInMaintenance() {
        loadTheDemo();

        final var spare = catalog.findEquipment(new ResourceId("i-drone-2")).orElseThrow();
        assertThat(spare.isAvailableDuring(period("2026-11-03T10:00", "2026-11-03T13:30"))).isFalse();
        assertThat(spare.isAvailableDuring(period("2026-11-04T15:00", "2026-11-04T16:30"))).isTrue();
    }

    @Test
    void schedulesTenActivitiesOfTheSixTypes() {
        final var expedition = theDemoExpedition();

        assertThat(expedition.getItinerary().getItems()).hasSize(10);
        assertThat(THE_TEN_ACTIVITIES).allSatisfy(scheduled -> {
            final var activity = itemOf(expedition, scheduled.activityId()).getActivity();
            assertThat(activity.estimatedDuration()).isEqualTo(Duration.ofMinutes(scheduled.durationMinutes()));
            assertThat(activity.estimatedRisk()).isEqualTo(scheduled.risk());
            assertThat(activity.requiredEquipment()).extracting(ReusableRequirement::category)
                    .contains(new ResourceCategory(scheduled.equipmentCategory()));
        });
        assertThat(THE_TEN_ACTIVITIES).extracting(Scheduled::equipmentCategory).containsOnly(
                "Truck", "Multiparameter probe", "Boat", "Drone", "Cooler", "Binoculars");
    }

    @Test
    void schedulesAParallelBlockInsideASequentialOne() {
        final var expedition = theDemoExpedition();

        final var block = expedition.getItinerary().getNodes().stream()
                .filter(node -> node.leaves().size() > 1)
                .findFirst()
                .orElseThrow();
        assertThat(block.leaves()).hasSize(4);
        assertThat(block.runsInParallel(itemOf(expedition, "a-03"), itemOf(expedition, "a-04"))).isTrue();
        assertThat(block.runsInParallel(itemOf(expedition, "a-02"), itemOf(expedition, "a-05"))).isFalse();
        assertThat(block.estimatedDuration()).isEqualTo(Duration.ofMinutes(90 + 210 + 180));
    }

    @Test
    void sharesResourcesBetweenActivitiesThatDoNotOverlap() {
        final var expedition = theDemoExpedition();

        assertThat(assignedTo(expedition, "a-05")).contains("p-lee", "i-cooler-1");
        assertThat(assignedTo(expedition, "a-08")).contains("p-lee", "i-cooler-1");
        assertThat(assignedTo(expedition, "a-04")).contains("p-alan", "i-drone-1");
        assertThat(assignedTo(expedition, "a-09")).contains("p-alan", "i-drone-1");
        assertThat(assignedTo(expedition, "a-01")).contains("p-nils", "v-truck-1");
        assertThat(assignedTo(expedition, "a-11")).contains("p-nils", "v-truck-1");
    }

    @Test
    void leavesAPlanWithNoCriticalAndNoWarningOfItsOwn() {
        final var result = validate.validate(theDemoExpedition().getId());

        assertThat(result.criticals()).isEmpty();
        assertThat(result.warnings()).isEmpty();
        assertThat(result.canBeApproved()).isTrue();
    }

    @Test
    void leavesTheNightActivityOfTheScriptOneCriticalAndOneWarningAway() {
        final var expedition = theDemoExpedition();

        assertThat(expedition.getGrantedPermits()).contains(new Permit("night operations in zone z-coast"));
        assertThat(expedition.getRestrictions().riskTolerance()).isEqualTo(RiskLevel.HIGH);
        assertThat(List.of("p-ada", "p-grace")).allSatisfy(diver ->
                assertThat(catalog.findStaff(new ResourceId(diver)).orElseThrow()
                        .hasCertification(new Certification("Night operations"))).isTrue());
        assertThat(catalog.findEquipment(new ResourceId("i-lamp-1")).orElseThrow()
                .belongsTo(new ResourceCategory("Lighting"))).isTrue();
        assertThat(expedition.getItinerary().itemFor(new ActivityId("a-10"))).isEmpty();
    }

    @Test
    void failsLoudlyWhenADocumentIsNotThere() {
        assertThatThrownBy(() -> loader.load(document("demo/catalog.json"), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("document is required");
    }

    private List<ExpeditionId> loadTheDemo() {
        return loader.load(document("demo/catalog.json"), document("demo/expedition.json"));
    }

    private Expedition theDemoExpedition() {
        final var loaded = loadTheDemo();
        assertThat(loaded).hasSize(1);
        return expeditions.findById(loaded.getFirst()).orElseThrow();
    }

    private ItineraryItem itemOf(Expedition expedition, String activityId) {
        return expedition.getItinerary().itemFor(new ActivityId(activityId)).orElseThrow();
    }

    private List<String> assignedTo(Expedition expedition, String activityId) {
        final var item = itemOf(expedition, activityId);
        return Stream.concat(item.getAssignedReusableResources().stream(), item.getAssignedSupplies().stream())
                .map(resource -> resource.getId().value())
                .toList();
    }

    private TimePeriod period(String start, String end) {
        return new TimePeriod(LocalDateTime.parse(start), LocalDateTime.parse(end));
    }

    private InputStream document(String path) {
        return Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(path), path);
    }

    private record Scheduled(String activityId, String equipmentCategory, long durationMinutes, RiskLevel risk) {
    }
}
