package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExpeditionBuilder {

    private final List<String> objectives = new ArrayList<>();
    private final Set<Zone> zones = new HashSet<>();
    private final Set<Person> responsibles = new HashSet<>();
    private final Set<Permit> grantedPermits = new HashSet<>();
    private final List<PlannedActivity> plannedActivities = new ArrayList<>();
    private String id;
    private String name;
    private TimePeriod period;
    private ExpeditionRestrictions restrictions;

    public ExpeditionBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public ExpeditionBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ExpeditionBuilder withPeriod(TimePeriod period) {
        this.period = period;
        return this;
    }

    public ExpeditionBuilder withRestrictions(ExpeditionRestrictions restrictions) {
        this.restrictions = restrictions;
        return this;
    }

    public ExpeditionBuilder addObjective(String objective) {
        objectives.add(objective);
        return this;
    }

    public ExpeditionBuilder addZone(Zone zone) {
        zones.add(zone);
        return this;
    }

    public ExpeditionBuilder addResponsible(Person responsible) {
        responsibles.add(responsible);
        return this;
    }

    public ExpeditionBuilder addPermit(Permit permit) {
        grantedPermits.add(permit);
        return this;
    }

    public ExpeditionBuilder addActivity(Activity activity, TimePeriod scheduledPeriod) {
        plannedActivities.add(new PlannedActivity(activity, scheduledPeriod));
        return this;
    }

    public Expedition build() {
        final var expedition = new Expedition(id, name, period, objectives, zones, responsibles, restrictions, grantedPermits);
        plannedActivities.forEach(planned -> expedition.schedule(planned.activity(), planned.scheduledPeriod()));
        return expedition;
    }

    private record PlannedActivity(Activity activity, TimePeriod scheduledPeriod) {
    }
}
