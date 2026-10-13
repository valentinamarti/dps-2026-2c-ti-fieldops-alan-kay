package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.interfaces.validation.ValidationRule;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.models.validation.ApprovalResult;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;
import ar.edu.itba.dps.fieldops.business.validations.ApprovalPolicy;

import java.util.List;
import java.util.Objects;

public class ValidateExpedition {
    private final ExpeditionRepository repository;
    private final List<ValidationRule> additionalRules;

    public ValidateExpedition(ExpeditionRepository repository, List<ValidationRule> additionalRules) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
        this.additionalRules = List.copyOf(Objects.requireNonNull(additionalRules, "additionalRules are required"));
    }

    public ApprovalResult validate(ExpeditionId id) {
        return new ApprovalPolicy(additionalRules).validate(Lookups.expedition(repository, id));
    }
}
