package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.estimations.ExpeditionEstimator;
import ar.edu.itba.dps.fieldops.business.models.estimation.ExpeditionEstimate;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;

import java.util.Objects;

public class EstimateExpedition {

    private final ExpeditionRepository repository;
    private final ExpeditionEstimator estimator;

    public EstimateExpedition(ExpeditionRepository repository, ExpeditionEstimator estimator) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
        this.estimator = Objects.requireNonNull(estimator, "estimator is required");
    }

    public ExpeditionEstimate estimate(ExpeditionId id) {
        return estimator.estimate(Lookups.expedition(repository, id));
    }
}
