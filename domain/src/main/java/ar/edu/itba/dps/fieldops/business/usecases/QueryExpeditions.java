package ar.edu.itba.dps.fieldops.business.usecases;

import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;
import ar.edu.itba.dps.fieldops.business.providers.ExpeditionRepository;

import java.util.List;
import java.util.Objects;

public class QueryExpeditions {

    private final ExpeditionRepository repository;

    public QueryExpeditions(ExpeditionRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
    }

    public Expedition get(ExpeditionId id) {
        return Lookups.expedition(repository, id);
    }

    public List<Expedition> list() {
        return repository.findAll();
    }
}
