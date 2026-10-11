package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionBuilder;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;

import java.util.Objects;

public class CreateExpedition {

    private final ExpeditionRepository repository;

    public CreateExpedition(ExpeditionRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
    }

    public Expedition create(ExpeditionBuilder draft) {
        final var expedition = draft.build(repository);
        repository.add(expedition);
        return expedition;
    }
}
