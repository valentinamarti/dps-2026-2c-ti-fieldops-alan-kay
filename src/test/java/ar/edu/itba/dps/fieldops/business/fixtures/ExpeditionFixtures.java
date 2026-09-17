package ar.edu.itba.dps.fieldops.business.fixtures;

import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.activities.RiskLevel;
import ar.edu.itba.dps.fieldops.business.models.common.Quantity;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionBuilder;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionRestrictions;
import ar.edu.itba.dps.fieldops.business.models.resources.Certification;
import ar.edu.itba.dps.fieldops.business.models.resources.Depletable;
import ar.edu.itba.dps.fieldops.business.models.resources.Instrument;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCategory;
import ar.edu.itba.dps.fieldops.business.models.resources.Vehicle;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.rules.DivingRules;
import ar.edu.itba.dps.fieldops.business.rules.SampleCollectionRules;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public final class ExpeditionFixtures {

    public static final Certification DIVING = new Certification("Diving");

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

    public static Vehicle vehicle(String id, String category) {
        return new Vehicle(id, category + " " + id, new ResourceCategory(category), "AA " + id, Set.of());
    }

    public static Instrument instrument(String id, String category) {
        return new Instrument(id, category + " " + id, new ResourceCategory(category), Set.of());
    }

    public static Depletable supply(String id, String category, Quantity stock) {
        return new Depletable(id, category + " " + id, new ResourceCategory(category), stock);
    }

    public static Activity sampling(String id, Zone zone, TimePeriod window, Activity... dependencies) {
        return new Activity(id, "Sampling " + id, window, zone, List.of(dependencies), new SampleCollectionRules(3));
    }

    public static Activity diving(String id, Zone zone, TimePeriod window, int maxDepthMeters) {
        return new Activity(id, "Diving " + id, window, zone, List.of(), new DivingRules(maxDepthMeters));
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
