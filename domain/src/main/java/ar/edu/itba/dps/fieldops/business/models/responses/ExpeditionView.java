package ar.edu.itba.dps.fieldops.business.models.responses;

import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionRestrictions;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionStatus;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public record ExpeditionView(ExpeditionId id, String name, ExpeditionStatus status, TimePeriod period,
                             List<String> objectives, Set<String> zoneIds, Set<ResourceId> responsibleIds,
                             Set<Permit> grantedPermits, ExpeditionRestrictions restrictions,
                             Optional<ExpeditionId> replacedExpedition, Optional<ExpeditionId> replacement,
                             boolean staleAlternative) {

    public static ExpeditionView of(Expedition expedition) {
        return of(expedition, false);
    }

    public static ExpeditionView of(Expedition expedition, boolean staleAlternative) {
        return new ExpeditionView(
                expedition.getId(),
                expedition.getName(),
                expedition.getStatus(),
                expedition.getPeriod(),
                expedition.getObjectives(),
                expedition.getZones().stream().map(Zone::getId).collect(Collectors.toUnmodifiableSet()),
                expedition.getResponsibles().stream().map(Person::getId).collect(Collectors.toUnmodifiableSet()),
                expedition.getGrantedPermits(),
                expedition.getRestrictions(),
                expedition.getReplacedExpedition(),
                expedition.getReplacement(),
                staleAlternative);
    }
}
