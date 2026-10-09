package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.interfaces.validation.ExpeditionValidator;
import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.validation.AcceptedWarning;
import ar.edu.itba.dps.fieldops.business.services.ResourceReservations;

import java.util.List;
import java.util.Objects;

public class ApproveExpedition {

    private final ExpeditionValidator validator;
    private final ResourceReservations reservations;

    public ApproveExpedition(ExpeditionValidator validator, ResourceReservations reservations) {
        this.validator = Objects.requireNonNull(validator, "validator is required");
        this.reservations = Objects.requireNonNull(reservations, "reservations are required");
    }

    public void approve(Expedition expedition, List<AcceptedWarning> acceptances) {
        reservations.requireReservable(expedition);
        expedition.approve(validator, acceptances);
        reservations.reserveFor(expedition);
    }
}
