package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.validation.AcceptedWarning;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.services.ResourceReservations;
import ar.edu.itba.dps.fieldops.business.validations.ApprovalPolicy;

import java.util.List;
import java.util.Objects;

public class ApproveExpedition {

    private final ApprovalPolicy policy;
    private final ResourceReservations reservations;
    private final ExpeditionRepository repository;

    public ApproveExpedition(ApprovalPolicy policy, ResourceReservations reservations, ExpeditionRepository repository) {
        this.policy = Objects.requireNonNull(policy, "policy is required");
        this.reservations = Objects.requireNonNull(reservations, "reservations are required");
        this.repository = Objects.requireNonNull(repository, "repository is required");
    }

    public void approve(Expedition expedition, List<AcceptedWarning> acceptances) {
        final var replaced = expedition.getReplacedExpedition().map(this::findReplaced);
        replaced.ifPresent(Expedition::requireReplaceable);
        reservations.requireReservable(expedition);
        expedition.approve(policy, acceptances);
        replaced.ifPresent(reservations::releaseFor);
        reservations.reserveFor(expedition);
        replaced.ifPresent(original -> original.markReplacedBy(expedition.getId()));
    }

    private Expedition findReplaced(ExpeditionId id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("the replaced expedition %s is not registered".formatted(id)));
    }
}
