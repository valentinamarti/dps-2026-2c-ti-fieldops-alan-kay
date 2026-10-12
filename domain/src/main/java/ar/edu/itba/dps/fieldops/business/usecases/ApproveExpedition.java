package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.interfaces.validation.ValidationRule;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.exceptions.NotFoundException;
import ar.edu.itba.dps.fieldops.business.models.resources.ResourceCatalog;
import ar.edu.itba.dps.fieldops.business.models.validation.AcceptedWarning;
import ar.edu.itba.dps.fieldops.business.models.validation.ValidationResult;
import ar.edu.itba.dps.fieldops.business.validations.ApprovalPolicy;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.services.ResourceReservations;

import java.util.List;
import java.util.Objects;

public class ApproveExpedition {

    private final List<ValidationRule> additionalRules;
    private final ResourceReservations reservations;
    private final ExpeditionRepository repository;
    private final ResourceCatalog catalog;

    public ApproveExpedition(List<ValidationRule> additionalRules, ResourceReservations reservations,
                             ExpeditionRepository repository, ResourceCatalog catalog) {
        this.additionalRules = List.copyOf(Objects.requireNonNull(additionalRules, "additionalRules are required"));
        this.reservations = Objects.requireNonNull(reservations, "reservations are required");
        this.repository = Objects.requireNonNull(repository, "repository is required");
        this.catalog = Objects.requireNonNull(catalog, "catalog is required");
    }

    /**
     * Each acceptance has to match a warning the expedition raises right now: accepting one that does not exist is an error.
     */
    public void approve(ExpeditionId id, List<WarningAcceptance> acceptances) {
        final var expedition = Lookups.expedition(repository, id);
        final var current = new ApprovalPolicy(additionalRules).validate(expedition).warnings();
        approve(expedition, acceptances.stream().map(acceptance -> accepted(acceptance, current)).toList());
    }

    private AcceptedWarning accepted(WarningAcceptance acceptance, List<ValidationResult> current) {
        final var warning = current.stream()
                .filter(candidate -> candidate.rule().equals(acceptance.rule()) && candidate.subject().equals(acceptance.subject()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("warning", "%s about %s".formatted(acceptance.rule(), acceptance.subject())));
        return new AcceptedWarning(warning, Lookups.staff(catalog, acceptance.responsibleId()), acceptance.justification());
    }

    public void approve(Expedition expedition, List<AcceptedWarning> acceptances) {
        final var replaced = expedition.getReplacedExpedition().map(this::findReplaced);
        replaced.ifPresent(Expedition::requireReplaceable);
        reservations.requireReservable(expedition);
        expedition.approve(additionalRules, acceptances);
        replaced.ifPresent(reservations::releaseFor);
        reservations.reserveFor(expedition);
        replaced.ifPresent(original -> original.markReplacedBy(expedition.getId()));
    }

    private Expedition findReplaced(ExpeditionId id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("the replaced expedition %s is not registered".formatted(id)));
    }
}
