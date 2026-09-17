package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.exceptions.InvalidScheduleException;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.expeditionIn;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.hours;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.person;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.sampling;
import static ar.edu.itba.dps.fieldops.business.fixtures.ExpeditionFixtures.zone;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpeditionBuilderTest {

    private static final ExpeditionRestrictions RESTRICTIONS = new ExpeditionRestrictions(10, RiskLevel.MEDIUM);

    private final Zone coast = zone("z-1");
    private final Person leader = person("p-1");

    @Test
    void buildsADraftWithTheGivenData() {
        final var expedition = expeditionIn(coast, leader).build();

        assertEquals(ExpeditionStatus.DRAFT, expedition.getStatus());
        assertEquals(List.of("Map the coastal reef"), expedition.getObjectives());
        assertEquals(Set.of(coast), expedition.getZones());
        assertEquals(Set.of(leader), expedition.getResponsibles());
        assertTrue(expedition.getItinerary().getItems().isEmpty());
    }

    @Test
    void schedulesTheActivitiesAddedToIt() {
        final var survey = sampling("a-1", coast, hours(8, 18));

        final var expedition = expeditionIn(coast, leader).addActivity(survey, hours(9, 11)).build();

        assertTrue(expedition.getItinerary().itemFor(survey).isPresent());
    }

    @Test
    void appliesTheItineraryRulesToTheActivitiesAddedToIt() {
        final var morningSurvey = sampling("a-1", coast, hours(8, 12));

        assertThrows(InvalidScheduleException.class,
                () -> expeditionIn(coast, leader).addActivity(morningSurvey, hours(11, 13)).build());
    }

    @Test
    void rejectsAnExpeditionWithoutNamePeriodOrRestrictions() {
        assertThrows(IllegalArgumentException.class, () -> expeditionIn(coast, leader).withName(" ").build());
        assertThrows(NullPointerException.class, () -> expeditionIn(coast, leader).withPeriod(null).build());
        assertThrows(NullPointerException.class, () -> expeditionIn(coast, leader).withRestrictions(null).build());
    }

    @Test
    void rejectsAnExpeditionWithoutObjectivesZonesOrResponsibles() {
        final var withoutObjectives = new ExpeditionBuilder().withId("e-1").withName("Survey").withPeriod(hours(0, 24))
                .withRestrictions(RESTRICTIONS).addZone(coast).addResponsible(leader);
        final var withoutZones = new ExpeditionBuilder().withId("e-1").withName("Survey").withPeriod(hours(0, 24))
                .withRestrictions(RESTRICTIONS).addObjective("Map the reef").addResponsible(leader);
        final var withoutResponsibles = new ExpeditionBuilder().withId("e-1").withName("Survey").withPeriod(hours(0, 24))
                .withRestrictions(RESTRICTIONS).addObjective("Map the reef").addZone(coast);

        assertThrows(IllegalArgumentException.class, withoutObjectives::build);
        assertThrows(IllegalArgumentException.class, withoutZones::build);
        assertThrows(IllegalArgumentException.class, withoutResponsibles::build);
    }
}