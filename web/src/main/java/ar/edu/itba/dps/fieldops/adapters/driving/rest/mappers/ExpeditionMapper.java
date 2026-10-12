package ar.edu.itba.dps.fieldops.adapters.driving.rest.mappers;

import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.CreateExpeditionRequest;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.ExpeditionResponse;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.PeriodDto;
import ar.edu.itba.dps.fieldops.adapters.driving.rest.dtos.RestrictionsDto;
import ar.edu.itba.dps.fieldops.business.models.common.TimePeriod;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                toPeriod(request.period()),
                Objects.requireNonNullElse(request.objectives(), List.of()),
                Objects.requireNonNullElse(request.zoneIds(), Set.of()),
                mapped(request.responsibleIds(), ResourceId::new),
                mapped(request.grantedPermits(), Permit::new),
                toRestrictions(request.restrictions()));
    }

    public static ExpeditionResponse toResponse(Expedition expedition) {
        final var restrictions = expedition.getRestrictions();
        return new ExpeditionResponse(
                expedition.getId().value(),
                expedition.getName(),
                expedition.getStatus(),
                toDto(expedition.getPeriod()),
                expedition.getObjectives(),
                sorted(expedition.getZones().stream().map(Zone::getId)),
                sorted(expedition.getResponsibles().stream().map(person -> person.getId().value())),
                sorted(expedition.getGrantedPermits().stream().map(Permit::name)),
                new RestrictionsDto(restrictions.maxParticipants(), restrictions.riskTolerance()),
                expedition.getReplacedExpedition().map(ExpeditionId::value).orElse(null),
                expedition.getReplacement().map(ExpeditionId::value).orElse(null));
    }

    private static TimePeriod toPeriod(PeriodDto period) {
        return period == null ? null : new TimePeriod(period.start(), period.end());
    }

    private static PeriodDto toDto(TimePeriod period) {
        return new PeriodDto(period.start(), period.end());
    }

    private static ExpeditionRestrictions toRestrictions(RestrictionsDto restrictions) {
        return restrictions == null
                ? null
                : new ExpeditionRestrictions(restrictions.maxParticipants(), restrictions.riskTolerance());
    }

    private static <T> Set<T> mapped(Set<String> values, Function<String, T> asValueObject) {
        return Objects.requireNonNullElse(values, Set.<String>of()).stream()
                .map(asValueObject)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static List<String> sorted(Stream<String> values) {
        return values.sorted().toList();
    }
}
