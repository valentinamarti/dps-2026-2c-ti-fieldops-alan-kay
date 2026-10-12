package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.activities.ActivityId;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.fixtures.SequentialExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.rules.ActivityTypeRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.NIGHT_OPERATIONS;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.nightPermit;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CreateExpeditionTest {

    private final SequentialExpeditionRepository repository = new SequentialExpeditionRepository();
    private final CreateExpedition createExpedition = ExpeditionFixtures.createExpeditionOn(repository);
    private final Zone coast = zone("z-1");
    private final Person leader = person("p-1");

    @Test
    void createsADraftThatCanBeFoundByTheIdTheSystemGaveIt() {
        final var expedition = createExpedition.create(expeditionIn(coast, leader));

        assertEquals(ExpeditionStatus.DRAFT, expedition.getStatus());
        assertSame(expedition, repository.findById(expedition.getId()).orElseThrow());
    }

    @Test
    void twoExpeditionsCreatedFromTheSameDataGetDifferentIds() {
        final var first = createExpedition.create(expeditionIn(coast, leader));
        final var second = createExpedition.create(expeditionIn(coast, leader));

        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void createsAnExpeditionWithANightActivity() {
        final var nightRules = ActivityTypeRegistry.withStandardTypes().rulesFor("diving", Map.of("maxDepthMeters", 20), true);
        final var nightDive = new Activity(new ActivityId("a-1"), "Night dive", hours(0, 24), coast, List.of(), nightRules);

        final var expedition = createExpedition.create(expeditionIn(coast, leader).addActivity(nightDive, hours(20, 22)));

        final var scheduled = expedition.getItinerary().itemFor(nightDive).orElseThrow();
        assertSame(expedition, repository.findById(expedition.getId()).orElseThrow());
        assertEquals(hours(20, 22), scheduled.getScheduledPeriod());
        assertEquals(RiskLevel.HIGH, scheduled.estimatedRisk());
        assertEquals(Set.of(nightPermit(coast)), nightDive.requiredPermits());
        assertEquals(Set.of(NIGHT_OPERATIONS), nightDive.requiredCertificationsForAllStaff());
    }
}
