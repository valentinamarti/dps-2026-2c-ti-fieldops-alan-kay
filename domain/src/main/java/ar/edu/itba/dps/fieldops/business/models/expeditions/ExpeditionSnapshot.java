package ar.edu.itba.dps.fieldops.business.models.expeditions;

import ar.edu.itba.dps.fieldops.business.interfaces.replanning.ReplanReason;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
import ar.edu.itba.dps.fieldops.business.models.resources.Person;
import ar.edu.itba.dps.fieldops.business.models.validation.AcceptedWarning;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record ExpeditionSnapshot(ExpeditionId id, String name, TimePeriod period, List<String> objectives, Set<Zone> zones,
                                 Set<Person> responsibles, ExpeditionRestrictions restrictions, Set<Permit> grantedPermits,
                                 ExpeditionStatus status, List<AcceptedWarning> acceptedWarnings, List<Incident> incidents,
                                 Optional<ReplacedPlan> replacedPlan, Optional<ReplanReason> replanReason,
                                 Optional<ExpeditionId> replacement, int executionRevision, List<NodeSnapshot> roots) {

    public ExpeditionSnapshot {
        Objects.requireNonNull(status, "status is required");
        acceptedWarnings = List.copyOf(acceptedWarnings);
        incidents = List.copyOf(incidents);
        Objects.requireNonNull(replacedPlan, "replacedPlan is required");
        Objects.requireNonNull(replanReason, "replanReason is required");
        Objects.requireNonNull(replacement, "replacement is required");
        roots = List.copyOf(roots);
    }
}
