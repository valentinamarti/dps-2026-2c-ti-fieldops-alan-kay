package ar.edu.itba.dps.fieldops;

import ar.edu.itba.dps.fieldops.business.estimations.ExpeditionEstimator;
import ar.edu.itba.dps.fieldops.business.replanning.ExpeditionReplanner;
import ar.edu.itba.dps.fieldops.business.reports.ExpeditionReporter;
import ar.edu.itba.dps.fieldops.business.services.ResourceReservations;
import ar.edu.itba.dps.fieldops.business.usecases.ApproveExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.CreateExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.FinishExpedition;
import ar.edu.itba.dps.fieldops.business.usecases.ReplanExpedition;
import ar.edu.itba.dps.fieldops.business.validations.ApprovalPolicy;
import ar.edu.itba.dps.fieldops.providers.InMemoryExpeditionRepository;

import java.util.List;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        wire();
    }

    public static FieldOps wire() {
        final var repository = new InMemoryExpeditionRepository();
        final var reservations = new ResourceReservations();
        return new FieldOps(
                new CreateExpedition(repository),
                new ApproveExpedition(new ApprovalPolicy(List.of()), reservations, repository),
                new ReplanExpedition(repository, new ExpeditionReplanner()),
                new FinishExpedition(reservations),
                new ExpeditionReporter(new ExpeditionEstimator()));
    }
}
