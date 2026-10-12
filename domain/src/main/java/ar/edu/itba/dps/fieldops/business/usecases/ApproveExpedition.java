package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.interfaces.validation.ValidationRule;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.validation.AcceptedWarning;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.services.ResourceReservations;

import java.util.List;
import java.util.Objects;

public class ApproveExpedition {

    private final List<ValidationRule> additionalRules;
    private final ResourceReservations reservations;
    private final ExpeditionRepository repository;

    public ApproveExpedition(List<ValidationRule> additionalRules, ResourceReservations reservations, ExpeditionRepository repository) {
        this.additionalRules = List.copyOf(Objects.requireNonNull(additionalRules, "additionalRules are required"));
        this.reservations = Objects.requireNonNull(reservations, "reservations are required");
        this.repository = Objects.requireNonNull(repository, "repository is required");
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
