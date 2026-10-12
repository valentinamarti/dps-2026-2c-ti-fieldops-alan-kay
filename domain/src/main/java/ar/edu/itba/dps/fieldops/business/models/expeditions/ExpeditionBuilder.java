package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.interfaces.replanning.ReplanReason;
import ar.edu.itba.dps.fieldops.business.models.activities.Activity;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ExpeditionBuilder {

    private final List<String> objectives = new ArrayList<>();
    private final Set<Zone> zones = new HashSet<>();
    private final Set<Person> responsibles = new HashSet<>();
    private final Set<Permit> grantedPermits = new HashSet<>();
    private final List<PlannedNode> plannedNodes = new ArrayList<>();
    private final List<Incident> inheritedIncidents = new ArrayList<>();
    private String name;
    private TimePeriod period;
    private ExpeditionRestrictions restrictions;
    private ExpeditionId replacedExpedition;
    private ReplanReason replanReason;

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
        return addNode(PlannedNode.activity(activity, scheduledPeriod));
    }

    public ExpeditionBuilder addNode(PlannedNode node) {
        plannedNodes.add(node);
        return this;
    }

    public ExpeditionBuilder withReplacedExpedition(ExpeditionId original) {
        this.replacedExpedition = original;
        return this;
    }

    public ExpeditionBuilder withReplanReason(ReplanReason replanReason) {
        this.replanReason = replanReason;
        return this;
    }

    /**
     * Incidents an alternative keeps from the expedition it replaces, so its report still tells the whole trip.
     */
    public ExpeditionBuilder addInheritedIncident(Incident incident) {
        inheritedIncidents.add(incident);
        return this;
    }

    public ExpeditionBuilder addFinishedItem(ItineraryItem finished) {
        return addNode(PlannedNode.finished(finished));
    }

    public Expedition build(ExpeditionRepository repository) {
        Objects.requireNonNull(repository, "repository is required");
        final var expedition = new Expedition(repository.nextId(), name, period, objectives, zones, responsibles, restrictions, grantedPermits, replacedExpedition, replanReason, inheritedIncidents);
        plannedNodes.forEach(expedition::schedule);
        return expedition;
    }
}
