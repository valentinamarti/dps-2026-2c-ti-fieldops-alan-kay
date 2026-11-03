package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.CreateExpeditionRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ExpeditionResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.RestrictionsDto;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionRestrictions;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.responses.ExpeditionView;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.models.commands.CreateExpeditionCommand;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ExpeditionMapper {
    private ExpeditionMapper() {
    }

    public static CreateExpeditionCommand toCommand(CreateExpeditionRequest request) {
        return new CreateExpeditionCommand(
                request.name(),
                Mappings.toPeriod(request.period()),
                Objects.requireNonNullElse(request.objectives(), List.of()),
                Objects.requireNonNullElse(request.zoneIds(), Set.of()),
                Mappings.mapped(request.responsibleIds(), ResourceId::new),
                Mappings.mapped(request.grantedPermits(), Permit::new),
                toRestrictions(request.restrictions()));
    }

    public static ExpeditionResponse toResponse(ExpeditionView expedition) {
        final var restrictions = expedition.restrictions();
        return new ExpeditionResponse(
                expedition.id().value(),
                expedition.name(),
                expedition.status(),
                Mappings.toDto(expedition.period()),
                expedition.objectives(),
                Mappings.sorted(expedition.zoneIds().stream()),
                Mappings.sorted(expedition.responsibleIds().stream().map(ResourceId::value)),
                Mappings.sorted(expedition.grantedPermits().stream().map(Permit::name)),
                new RestrictionsDto(restrictions.maxParticipants(), restrictions.riskTolerance()),
                expedition.replacedExpedition().map(ExpeditionId::value).orElse(null),
                expedition.replacement().map(ExpeditionId::value).orElse(null),
                expedition.staleAlternative());
    }

    private static ExpeditionRestrictions toRestrictions(RestrictionsDto restrictions) {
        return restrictions == null
                ? null
                : new ExpeditionRestrictions(restrictions.maxParticipants(), restrictions.riskTolerance());
    }
}
