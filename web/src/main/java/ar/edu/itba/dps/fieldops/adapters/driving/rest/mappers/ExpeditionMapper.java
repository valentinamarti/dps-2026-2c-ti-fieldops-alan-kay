package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.CreateExpeditionRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ExpeditionResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.RestrictionsDto;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionRestrictions;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceId;
import ar.edu.itba.dps.fieldops.business.models.zones.Permit;
import ar.edu.itba.dps.fieldops.business.models.zones.Zone;
import ar.edu.itba.dps.fieldops.business.usecases.CreateExpeditionCommand;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Translates between the expedition DTOs and the domain, in both directions. It is the only place in the
 * adapter that knows both vocabularies (D49).
 *
 * <p>It does not validate: a field that is missing travels as {@code null} or empty and the domain rejects it
 * with its own message, which the error handler turns into a 400.
 */
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

    public static ExpeditionResponse toResponse(Expedition expedition) {
        final var restrictions = expedition.getRestrictions();
        return new ExpeditionResponse(
                expedition.getId().value(),
                expedition.getName(),
                expedition.getStatus(),
                Mappings.toDto(expedition.getPeriod()),
                expedition.getObjectives(),
                Mappings.sorted(expedition.getZones().stream().map(Zone::getId)),
                Mappings.sorted(expedition.getResponsibles().stream().map(person -> person.getId().value())),
                Mappings.sorted(expedition.getGrantedPermits().stream().map(Permit::name)),
                new RestrictionsDto(restrictions.maxParticipants(), restrictions.riskTolerance()),
                expedition.getReplacedExpedition().map(ExpeditionId::value).orElse(null),
                expedition.getReplacement().map(ExpeditionId::value).orElse(null));
    }

    private static ExpeditionRestrictions toRestrictions(RestrictionsDto restrictions) {
        return restrictions == null
                ? null
                : new ExpeditionRestrictions(restrictions.maxParticipants(), restrictions.riskTolerance());
    }
}
