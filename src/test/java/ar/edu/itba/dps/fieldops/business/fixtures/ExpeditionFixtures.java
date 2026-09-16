package ar.edu.itba.dps.fieldops.business.fixtures;

import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionBuilder;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionRestrictions;
import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.rules.SampleCollectionRules;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public final class ExpeditionFixtures {

    private static final LocalDateTime FIRST_DAY = LocalDateTime.of(2026, 10, 1, 0, 0);

    private ExpeditionFixtures() {
    }

    public static TimePeriod hours(int fromHour, int toHour) {
        return new TimePeriod(FIRST_DAY.plusHours(fromHour), FIRST_DAY.plusHours(toHour));
    }

    public static Zone zone(String id) {
        return new Zone(id, "Zone " + id, Set.of());
    }

    public static Person person(String id, Certification... certifications) {
        return new Person(id, "Person " + id, Set.of(certifications));
    }

    public static Activity sampling(String id, Zone zone, TimePeriod window, Activity... dependencies) {
        return new Activity(id, "Sampling " + id, window, zone, List.of(dependencies), new SampleCollectionRules(3));
    }

    public static ExpeditionBuilder expeditionIn(Zone zone, Person responsible) {
        return new ExpeditionBuilder()
                .withId("e-1")
                .withName("Coastal survey")
                .withPeriod(hours(0, 24))
                .addObjective("Map the coastal reef")
                .addZone(zone)
                .addResponsible(responsible)
                .withRestrictions(new ExpeditionRestrictions(10, RiskLevel.MEDIUM));
    }
}
