package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.validation.AcceptedWarning;
import ar.edu.itba.dps.fieldops.business.services.ResourceReservations;
import ar.edu.itba.dps.fieldops.business.validations.ApprovalPolicy;

import java.util.List;
import java.util.Objects;

public class ApproveExpedition {

    private final ApprovalPolicy policy;
    private final ResourceReservations reservations;

    public ApproveExpedition(ApprovalPolicy policy, ResourceReservations reservations) {
        this.policy = Objects.requireNonNull(policy, "policy is required");
        this.reservations = Objects.requireNonNull(reservations, "reservations are required");
    }

    public void approve(Expedition expedition, List<AcceptedWarning> acceptances) {
        reservations.requireReservable(expedition);
        expedition.approve(policy, acceptances);
        reservations.reserveFor(expedition);
    }
}
