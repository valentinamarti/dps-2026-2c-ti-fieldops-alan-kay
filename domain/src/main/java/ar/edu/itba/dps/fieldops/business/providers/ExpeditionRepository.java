package ar.edu.itba.dps.fieldops.business.providers;

import ar.edu.itba.dps.fieldops.business.models.expeditions.Expedition;
import ar.edu.itba.dps.fieldops.business.models.expeditions.ExpeditionId;

import java.util.List;
import java.util.Optional;

public interface ExpeditionRepository extends ExpeditionIdProvider {

    void add(Expedition expedition);

    void save(Expedition expedition);

    Optional<Expedition> findById(ExpeditionId id);

    List<Expedition> findAll();
}
